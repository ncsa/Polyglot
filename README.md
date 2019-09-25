# Polyglot

Utilizing a tool called a Software Server to program against functionality within arbitrary 3rd party software, Polyglot is a distributed service which carries out file format conversions utilizing the open, save, import, and export capabilities amongst a dynamic and extensible collection of available software. Polyglot address the need to access content amongst the many possible formats available to store data digitally. Polyglot also addresses the problem of information loss that inevitably occurs through conversion and provides means of quantifying that loss and then minimizing it during future conversions.

* Project Page: https://opensource.ncsa.illinois.edu/projects/POL
* Documentation: http://opensource.ncsa.illinois.edu/projects/artifacts/POL/2.1.0-SNAPSHOT/documentation/manual/

## Systemd setup

To setup polyglot as a service in systemd, simply modify the bin/polyglot.service file so that WorkingDirctory and ExecStart point to the Polyglot install directory. Then copy or link the bin/polyglot.service file into /etc/systemd/system/. Now you can utilize normal systemd service commands to start/stop/enable/disable the polyglot service.
