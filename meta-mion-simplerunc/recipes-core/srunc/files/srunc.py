#! /usr/bin/env python3
#
# srunc
#
# Copyright (C) 2017-2019 Togán Labs
# SPDX-License-Identifier: MIT
#

# Disable a bunch of pylint checks for now
# pylint: disable=missing-docstring,no-self-use,fixme,too-many-public-methods

import cmd
import configparser
import fcntl
import json
import logging
import os
import shlex
import shutil
import subprocess
import sys
import tarfile
import time
import urllib.request

from datetime import datetime

APP_NAME = "srunc"
VERSION_STRING = "0.4"

def get_image_config(image_root):
    image_url = os.path.join(image_root, "image_guest.json")

    logging.debug("Retrieving \"%s\"...", image_url)
    image_json = urllib.request.urlopen(image_url).read().decode('utf-8')
    return json.loads(image_json)

def install_rootfs(rootfs_url, local_path):
    rootfs_path = os.path.join(local_path, "rootfs")

    logging.debug("Retrieving \"%s\"...", rootfs_url)
    (rootfs_filename, _) = urllib.request.urlretrieve(rootfs_url)
    logging.debug("Extracting to \"%s\"...", rootfs_path)
    with tarfile.open(rootfs_filename, mode="r:xz") as tarball:
        tarball.extractall(rootfs_path)
    urllib.request.urlcleanup()

def create_spec_file(name, local_path, command, capabilities):
    spec_path = os.path.join(local_path, "config.json")
    logging.debug("Creating spec file \"%s\"...", spec_path)
    subprocess.run(["runc", "spec"], cwd=local_path, check=True)
    spec_file = open(spec_path, 'r+')
    spec = json.load(spec_file)

    # Add netns hook
    if not "hooks" in spec:
        spec['hooks'] = {}
    if not "prestart" in spec["hooks"]:
        spec['hooks']['prestart'] = []
    netns_hook = {'path': '/usr/sbin/netns'}
    spec['hooks']['prestart'].append(netns_hook)

    # Make rootfs writable
    spec['root']['readonly'] = False

    # Set hostname to the container name
    spec['hostname'] = name

    # Use dumb-init as PID 1 within the container and have this program
    # launch the desired command
    command_args = shlex.split(command)
    spec['process']['args'] = ['/sbin/dumb-init'] + command_args
    spec['process']['terminal'] = False

    # Define the capability sets for the container
    spec['process']['capabilities']['effective'] = capabilities
    spec['process']['capabilities']['bounding'] = capabilities
    spec['process']['capabilities']['inheritable'] = capabilities
    spec['process']['capabilities']['permitted'] = capabilities
    spec['process']['capabilities']['ambient'] = capabilities

    # Add tmpfs mounts
    spec['mounts'].append({
        "destination": "/run",
        "type": "tmpfs",
        "source": "tmpfs",
        "options": [
            "mode=0755",
            "nodev",
            "nosuid",
            "strictatime"
            ]
        })
    spec['mounts'].append({
        "destination": "/var/volatile",
        "type": "tmpfs",
        "source": "tmpfs"
        })

    # FIXME Fix permissions and add device nodes
    spec['linux']['resources']['devices'][0]['allow'] = True
    spec['linux']['devices'] = [{
            "path": "/dev/ipmi0",
            "type": "c",
            "major": 248,
            "minor": 0,
            "uid": 0,
            "gid": 0
        },
        {
            "path": "/dev/i2c-2",
            "type": "c",
            "major": 89,
            "minor": 2,
            "uid": 0,
            "gid": 0
        }]

    # Write back the updated spec
    spec_file.seek(0)
    spec_file.truncate()
    json.dump(spec, spec_file, indent=4)
    spec_file.write("\n")
    spec_file.close()

class SruncSysmgr:
    def __init__(self):
        self.statefile = None

    def add_source(self, name, url):
        state = self._lock_and_read_state()
        if "sources" in state:
            if name in state['sources']:
                logging.error("Source %s already defined!", name)
                return
        else:
            state['sources'] = {}

        state['sources'][name] = {
            'url': url
        }
        self._unlock_and_write_state(state)
        logging.info("Added source \"%s\" with URL \"%s\"", name, url)

    def remove_source(self, name):
        state = self._lock_and_read_state()
        if "sources" not in state:
            logging.error("Source %s not defined!", name)
            return
        if name not in state['sources']:
            logging.error("Source %s not defined!", name)
            return

        del state['sources'][name]
        self._unlock_and_write_state(state)
        logging.info("Removed source \"%s\"", name)

    def list_sources(self):
        state = self._lock_and_read_state()
        self._unlock_and_discard_state()
        if "sources" not in state:
            return

        for name in state['sources']:
            print(name)

    def show_source(self, name):
        state = self._lock_and_read_state()
        self._unlock_and_discard_state()
        if "sources" not in state:
            logging.error("Source %s not defined!", name)
            return
        if name not in state['sources']:
            logging.error("Source %s not defined!", name)
            return

        print(json.dumps(state['sources'][name], indent=4, sort_keys=True))

    def add_guest(self, name, image):
        state = self._lock_and_read_state()
        if "guests" in state:
            if name in state['guests']:
                logging.error("Guest %s already defined!", name)
                return
        else:
            state['guests'] = {}

        # For now, image name must be fully qualified as "<source>:<image>". In
        # the future we should support unqualified image names which we will
        # search for in each configured source
        (source_name, image_name) = image.split(":")

        if source_name not in state['sources']:
            logging.error("Source %s not defined!", name)
            return

        source = state['sources'][source_name]

        image_root = os.path.join(source['url'], 'guest', image_name)
        image_config = get_image_config(image_root)

        rootfs_url = os.path.join(image_root, image_config['ROOTFS'])
        local_path = os.path.join("/var/lib/srunc-guests", name)
        install_rootfs(rootfs_url, local_path)
        create_spec_file(name, local_path, image_config['COMMAND'],
                         image_config['CAPABILITIES'])

        state['guests'][name] = {
            'image_name': image_name,
            'image': image_config,
            'source_name': source_name,
            'source': source,
            'path': local_path,
            'autostart_enabled': 0,
        }

        self._unlock_and_write_state(state)
        logging.info("Added guest \"%s\" from image \"%s\"", name, image)

    def remove_guest(self, name):
        state = self._lock_and_read_state()
        if "guests" not in state:
            logging.error("Guest %s not defined!", name)
            return
        if name not in state['guests']:
            logging.error("Guest %s not defined!", name)
            return

        guest_path = state['guests'][name]['path']
        logging.debug("Deleting data from \"%s\"...", guest_path)
        shutil.rmtree(guest_path)
        del state['guests'][name]
        self._unlock_and_write_state(state)
        logging.info("Removed guest \"%s\"", name)

    def list_guests(self):
        state = self._lock_and_read_state()
        self._unlock_and_discard_state()
        if "guests" not in state:
            return

        for name in state['guests']:
            print(name)

    def show_guest(self, name):
        state = self._lock_and_read_state()
        self._unlock_and_discard_state()
        if "guests" not in state:
            logging.error("Guest %s not defined!", name)
            return
        if name not in state['guests']:
            logging.error("Guest %s not defined!", name)
            return

        print(json.dumps(state['guests'][name], indent=4, sort_keys=True))

    def enable_guest(self, name):
        state = self._lock_and_read_state()
        if "guests" not in state:
            logging.error("Guest %s not defined!", name)
            return
        if name not in state['guests']:
            logging.error("Guest %s not defined!", name)
            return
        if state['guests'][name]['autostart_enabled'] == 1:
            logging.error("Guest %s already enabled!", name)
            return

        state['guests'][name]['autostart_enabled'] = 1

        self._unlock_and_write_state(state)
        logging.info("Enabled guest \"%s\"", name)

    def disable_guest(self, name):
        state = self._lock_and_read_state()
        if "guests" not in state:
            logging.error("Guest %s not defined!", name)
            return
        if name not in state['guests']:
            logging.error("Guest %s not defined!", name)
            return
        if state['guests'][name]['autostart_enabled'] == 0:
            logging.error("Guest %s already disabled!", name)
            return

        state['guests'][name]['autostart_enabled'] = 0

        self._unlock_and_write_state(state)
        logging.info("Disabled guest \"%s\"", name)

    def start_guest(self, name):
        runc_args = ["run", "-d", name]
        log_path = os.path.join("/var/lib/srunc-guests", name, "log")

        with open(log_path, "a") as logfile:
            timestamp = datetime.now().isoformat()
            logfile.write(">>> Starting guest \"%s\" at %s\n" % (name, timestamp))
            logfile.flush()
            self.runc(name, runc_args, stdin=subprocess.DEVNULL, stdout=logfile,
                      stderr=subprocess.STDOUT)

        logging.info("Started guest \"%s\"", name)

    def stop_guest(self, name):
        timeout = 10
        runc_args = ["kill", name, "TERM"]
        try:
            self.runc(name, runc_args)
            logging.info("Sent SIGTERM, waiting for %d seconds", timeout)
            time.sleep(timeout)
        except subprocess.CalledProcessError as err:
            logging.info("Failed to send SIGTERM: %s", err)
            logging.info("Deleting guest immediately")

        runc_args = ["delete", "-f", name]
        self.runc(name, runc_args)
        logging.info("Stopped guest \"%s\"", name)

    def preconfigure(self):
        if os.path.exists('/var/lib/srunc-guests/preconfigure-done'):
            logging.debug("Preconfiguration already done")
            return

        if not os.path.exists('/usr/share/srunc/preconfig.d'):
            logging.debug("No preconfiguration data")
            return

        logging.debug("Preconfiguration needed")
        os.makedirs("/var/lib/srunc-guests", exist_ok=True)
        open('/var/lib/srunc-guests/preconfigure-done', 'w').close()

        logging.debug("Loading preconfiguration data...")
        preconfig = configparser.ConfigParser()
        conf_list = os.listdir('/usr/share/srunc/preconfig.d')
        conf_list.sort()
        for fname in conf_list:
            path = os.path.join('/usr/share/srunc/preconfig.d', fname)
            preconfig.read(path)

        logging.debug("Setting up sources...")
        for section in preconfig.sections():
            if section.startswith('source:'):
                name = section.split(':', 1)[1]
                url = preconfig.get(section, 'url')
                self.add_source(name, url)

        logging.debug("Setting up guests...")
        for section in preconfig.sections():
            if section.startswith('guest:'):
                name = section.split(':', 1)[1]
                image = preconfig.get(section, 'image')
                enable = preconfig.get(section, 'enable')
                self.add_guest(name, image)
                if enable.lower() in ['true', 'yes', '1']:
                    self.enable_guest(name)

    def autostart_all(self):
        state = self._lock_and_read_state()
        self._unlock_and_discard_state()
        if "guests" not in state:
            state['guests'] = {}

        count = 0
        count_success = 0
        for name in state['guests']:
            if state['guests'][name]['autostart_enabled'] == 1:
                count += 1
                try:
                    self.start_guest(name)
                    count_success += 1
                except subprocess.CalledProcessError as err:
                    logging.error("Failed to start guest \"%s\": %s", name, err)

        logging.info("Started %d of %d enabled guests", count_success, count)

    def autostop_all(self):
        state = self._lock_and_read_state()
        self._unlock_and_discard_state()
        if "guests" not in state:
            state['guests'] = {}

        count = 0
        count_success = 0
        for name in state['guests']:
            count += 1
            try:
                self.stop_guest(name)
                count_success += 1
            except subprocess.CalledProcessError as err:
                logging.info("Failed to stop guest \"%s\": %s", name, err)

        logging.info("Stopped %d of %d guests", count_success, count)

    def startup(self):
        self.preconfigure()
        self.autostart_all()

    def shutdown(self):
        self.autostop_all()

    def runc(self, name, runc_args, **kwargs):
        state = self._lock_and_read_state()
        self._unlock_and_discard_state()
        if "guests" not in state:
            logging.error("Guest %s not defined!", name)
            return
        if name not in state['guests']:
            logging.error("Guest %s not defined!", name)
            return

        local_path = os.path.join("/var/lib/srunc-guests", name)
        args = ["runc"] + runc_args
        subprocess.run(args, cwd=local_path, check=True, **kwargs)

    def _lock_and_read_state(self):
        try:
            logging.debug("Loading state...")
            self.statefile = open('/var/lib/srunc-guests/state', 'r+')
            fcntl.lockf(self.statefile, fcntl.LOCK_EX)
            return json.load(self.statefile)
        except OSError as err:
            logging.debug("Existing state cannot be opened: %s", err)
            logging.debug("Creating blank state...")
            os.makedirs("/var/lib/srunc-guests", exist_ok=True)
            self.statefile = open('/var/lib/srunc-guests/state', 'w')
            fcntl.lockf(self.statefile, fcntl.LOCK_EX)
            state = {}
            json.dump(state, self.statefile, indent=4)
            self.statefile.write("\n")
            return state

    def _unlock_and_write_state(self, state):
        logging.debug("Writing back state...")
        self.statefile.seek(0)
        self.statefile.truncate()
        json.dump(state, self.statefile, indent=4)
        self.statefile.write("\n")
        self.statefile.close()

    def _unlock_and_discard_state(self):
        logging.debug("Discarding state (read-only command)...")
        self.statefile.close()

class Srunc(cmd.Cmd):
    intro = "Welcome to %s (%s)" % (APP_NAME, VERSION_STRING)
    prompt = "srunc> "
    def __init__(self):
        self.sysmgr = SruncSysmgr()
        super().__init__()

    def do_add_source(self, line):
        """
        add_source NAME URL

        Register a new source from which images may be fetched.

        Arguments:

            NAME    An identifier which may be used to reference this source in
                    future commands.

            URL     The root URL under which image archives may be found.

        Example:

            add_source srunc https://example.com/path/to/guests
        """

        args = line.split()
        if len(args) != 2:
            logging.error("Incorrect number of args!")
            return
        (name, url) = args
        self.sysmgr.add_source(name, url)

    def do_remove_source(self, line):
        """
        remove_source NAME

        Remove a previously registered source.

        Arguments:

            NAME    The identifier of the source to remove.

        Example:

            remove_source srunc
        """

        args = line.split()
        if len(args) != 1:
            logging.error("Incorrect number of args!")
            return
        name = args[0]
        self.sysmgr.remove_source(name)

    def do_list_sources(self, line):
        """
        list_sources

        List all currently registered sources.

        Arguments:

            (none)

        Example:

            list_sources
        """

        args = line.split()
        if args:
            logging.error("Incorrect number of args!")
            return
        self.sysmgr.list_sources()

    def do_show_source(self, line):
        """
        show_source NAME

        Show details of a previously registered source in JSON format.

        Arguments:

            NAME    The identifier of the source to show.

        Example:

            show_source srunc
        """

        args = line.split()
        if len(args) != 1:
            logging.error("Incorrect number of args!")
            return
        name = args[0]
        self.sysmgr.show_source(name)

    def do_add_guest(self, line):
        """
        add_guest NAME IMAGE

        Create a new guest container from an image.

        Arguments:

            NAME    An identifier which may be used to reference this source in
                    future commands.

            IMAGE   A fully-qualified reference to an image which is available
                    from one of the sources which has been configured. The
                    format of this reference is "<source>:<image name>".

        Example:

            add_guest test srunc:minimal
        """
        args = line.split()
        if len(args) != 2:
            logging.error("Incorrect number of args!")
            return
        (name, image) = args

        self.sysmgr.add_guest(name, image)

    def do_remove_guest(self, line):
        """
        remove_guest NAME

        Delete an existing guest container.

        Arguments:

            NAME    The identifier of the guest container to remove.

        Example:

            remove_guest test
        """
        args = line.split()
        if len(args) != 1:
            logging.error("Incorrect number of args!")
            return
        name = args[0]

        self.sysmgr.remove_guest(name)

    def do_list_guests(self, line):
        """
        list_sources

        List all currently registered guests.

        Arguments:

            (none)

        Example:

            list_guests
        """

        args = line.split()
        if args:
            logging.error("Incorrect number of args!")
            return
        self.sysmgr.list_guests()

    def do_show_guest(self, line):
        """
        show_guest NAME

        Show details of a previously registered guest in JSON format.

        Arguments:

            NAME    The identifier of the guest to show.

        Example:

            show_guest test
        """

        args = line.split()
        if len(args) != 1:
            logging.error("Incorrect number of args!")
            return
        name = args[0]
        self.sysmgr.show_guest(name)

    def do_enable_guest(self, line):
        """
        enable_guest NAME

        Enable auto-start of a previously registered guest during system boot.

        Arguments:

            NAME    The identifier of the guest to enable.

        Example:

            enable_guest test
        """

        args = line.split()
        if len(args) != 1:
            logging.error("Incorrect number of args!")
            return
        name = args[0]
        self.sysmgr.enable_guest(name)

    def do_disable_guest(self, line):
        """
        disable_guest NAME

        Disable auto-start of a previously registered guest during system boot.

        Arguments:

            NAME    The identifier of the guest to disable.

        Example:

            disable_guest test
        """

        args = line.split()
        if len(args) != 1:
            logging.error("Incorrect number of args!")
            return
        name = args[0]
        self.sysmgr.disable_guest(name)

    def do_start_guest(self, line):
        """
        start_guest NAME

        Start an existing guest container. The container is launched in the
        background, without access to the terminal where start_guest was
        executed.

        Arguments:

            NAME    The identifier of the guest container to start.

        Example:

            start_guest test
        """
        args = line.split()
        if len(args) != 1:
            logging.error("Incorrect number of args!")
            return
        name = args[0]
        self.sysmgr.start_guest(name)

    def do_stop_guest(self, line):
        """
        stop_guest NAME

        Stop a running guest container. SIGTERM is sent to the container so that
        it can shutdown cleanly. After 10 seconds, the container is halted.

        Arguments:

            NAME    The identifier of the guest container to stop.

        Example:

            stop_guest test
        """
        args = line.split()
        if len(args) != 1:
            logging.error("Incorrect number of args!")
            return
        name = args[0]
        self.sysmgr.stop_guest(name)

    def do_preconfigure(self, line):
        """
        preconfigure

        Read pre-configuration data from `/usr/share/srunc/preconfig.d` and
        add the listed sources and guests.

        Arguments:

            (none)

        Example:

            preconfigure
        """
        args = line.split()
        if args:
            logging.error("Incorrect number of args!")
            return
        self.sysmgr.preconfigure()

    def do_autostart_all(self, line):
        """
        autostart_all

        Start all containers which have autostart enabled.

        Arguments:

            (none)

        Example:

            autostart_all
        """
        args = line.split()
        if args:
            logging.error("Incorrect number of args!")
            return
        self.sysmgr.autostart_all()

    def do_autostop_all(self, line):
        """
        autostop_all

        Stop all currently running containers.

        Arguments:

            (none)

        Example:

            autostop_all
        """
        args = line.split()
        if args:
            logging.error("Incorrect number of args!")
            return
        self.sysmgr.autostop_all()

    def do_startup(self, line):
        """
        startup

        Convenience function for use in systemd service file. Runs
        'preconfigure' then 'autostart_all'.

        Arguments:

            (none)

        Example:

            startup
        """
        args = line.split()
        if args:
            logging.error("Incorrect number of args!")
            return
        self.sysmgr.startup()

    def do_shutdown(self, line):
        """
        shutdown

        Convenience function for use in systemd service file. Runs
        'autostop_all'.

        Arguments:

            (none)

        Example:

            shutdown
        """
        args = line.split()
        if args:
            logging.error("Incorrect number of args!")
            return
        self.sysmgr.shutdown()

    def do_runc(self, line):
        """
        runc NAME ARGS...

        Execute 'runc' for an existing guest container. See the documentation of
        'runc' for further details.

        Arguments:

            NAME    The identifier of the guest container for which 'runc' will
                    be executed.

            ARGS... Command line arguments passed through to the 'runc'
                    application.

        Example:

            runc test spec
        """
        args = line.split()
        if not args:
            logging.error("Incorrect number of args!")
            return
        name = args[0]
        runc_args = args[1:]

        self.sysmgr.runc(name, runc_args)

    def do_version(self, _):
        """
        version

        Display version information.
        """
        print("%s (%s)" % (APP_NAME, VERSION_STRING))

    def do_exit(self, _):
        """
        exit

        Exit the interactive srunc shell.
        """
        return True

    def help_arguments(self):
        print("Command Line Arguments:")
        print("=======================")
        print()
        print("    -v/--verbose         Print verbose debug messages during operation")
        print("    -h/--help [topic]    Print help and exit")
        print("    -V/--version         Print version string and exit")

def main():
    # srunc is typically used interactively so keep log messages simple
    logging.basicConfig(level=logging.INFO, format="%(message)s")

    # Really dumb handling for '-v'/'--verbose' argument
    if len(sys.argv) > 1:
        if sys.argv[1] in ("-v", "--verbose"):
            logging.getLogger().setLevel(logging.DEBUG)
            del sys.argv[1]

    srunc = Srunc()
    if len(sys.argv) > 1:
        # Convert common option-style arguments into commands
        if sys.argv[1] in ("-h", "--help"):
            sys.argv[1] = "help"
        elif sys.argv[1] in ("-V", "--version"):
            sys.argv[1] = "version"

        line = ' '.join(sys.argv[1:])
        srunc.onecmd(line)
    else:
        srunc.cmdloop()

if __name__ == '__main__':
    main()
