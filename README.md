# freeslick-docker

Docker images for building/testing FreeSlick profiles.

These Docker images are necessarily private, because they contain
proprietary binaries from Microsoft and other third parties (e.g.
Oracle, SAP). However, all the instructions and scripts for recreating
the images are documented in this repository.

## Windows XP

The MSSQL instances are implemented as
[virtualbox](https://www.virtualbox.org/) images on top of Windows XP.
It is possible to obtain a gratis instance of Windows XP, albeit
highly time-consuming.

1. Download
   [XP Mode](http://www.microsoft.com/en-us/download/details.aspx?id=8002)
   from Microsoft and extract with `7z x`, and again on the file named `xpm` to
   produce `5189623a8e5c6ff518cdd4759037f109 VirtualXPVHD`, renaming
   to `VirtualXP.vhd`.
2. Download
   [VMLite's BIOS](http://www.vmlite.com/images/fbfiles/files/pcbios.zip)
   and extract `12ccdc652b30c6d1e307c6f7deff5d24 pcbios.bin`
3. Create a VirtualBox image called `WindowsXP` using the vhd file as
   the base, using 512MB RAM, setting the pcbios as the above with a
   command (on the host) such as
   `VBoxManage setextradata WindowsXP VBoxInternal/Devices/pcbios/0/Config/BiosRom $PWD/pcbios.bin`
4. VirtualBox should set up a NAT network adapter. Add port forwarding
   for ports `2000`, `2005` and `2008` on both host and guest without
   specifying any IP addresses.
5. Boot up the image and perform all the software updates, including
   the optional ones.
6. (Optional, and repeat after the various MSSQL installs below) Try
   your damnedest to
   [reduce the installation size](https://www.google.co.uk/?q=windows+xp+reduce+installation+size)
   (you can get it to about 3GB), then run
   [`sdelete -z`](https://technet.microsoft.com/en-gb/sysinternals/bb897443.aspx)
   in a console, allowing you to compact your image with
   `vboxmanage modifyhd /path/to/thedisk.vdi --compact` (you will need to convert
   the VHD to VDI format first).

## MSSQL 2000 (Desktop Edition)

1. Download [SQL Server 2000 Desktop Edition](http://www.microsoft.com/en-us/download/details.aspx?id=22661).
2. Install by typing `setup sapwd="FreeSlick" securitymode=sql`
3. run `C:/Program Files/Microsoft SQL
   Server/80/Tools/Binn/SVRNETCN.exe` and enable TCP on port 2000.
4. Test the JDBC connection from the host by attempting to connect to
   `jdbc:jtds:sqlserver://localhost:2000` with a tool in your
   host system, e.g.
   [Squirrel SQL](http://squirrel-sql.sourceforge.net/) with the
   [jTDS](http://jtds.sourceforge.net/) drivers for MS-SQL.

## MSSQL 2005

1. Start with a Windows XP image.
2. Download
   [SQL Server Express 2005 SP4](http://www.microsoft.com/en-gb/download/details.aspx?id=184)
   from Microsoft and install on the guest image. Make sure to select
   the "mixed mode" authentication, and the defaults for everything
   else. Select a password, we use `FreeSlick`.
3. When installed, use the manager to enable the TCP protocol on all
   interfaces. This should be as simple as deleting the default `0`
   from the "dynamic port" entry for "all interfaces" and explicitly adding port
   `2005`. You may remove the other protocol options, as we will not
   use them.
4. Add flag `-T8038` to the startup parameters as workaround for
   [virtualbox #3613](https://www.virtualbox.org/ticket/3613) more
   details about [clock timings on the MSDN blog](http://blogs.msdn.com/b/psssql/archive/2009/05/29/how-it-works-sql-server-timings-and-timer-output-gettickcount-timegettime-queryperformancecounter-rdtsc.aspx)
5. Test the JDBC connection from the host by attempting to connect to
   `jdbc:jtds:sqlserver://localhost:2005` with a tool in your
   host system, e.g.
   [Squirrel SQL](http://squirrel-sql.sourceforge.net/) with the
   [jTDS](http://jtds.sourceforge.net/) drivers for MS-SQL.

## MSSQL 2008

1. Start with a Windows XP image.
2. Download
   [SQL Server Express 2008](http://www.microsoft.com/en-gb/download/details.aspx?id=30438)
   from Microsoft and install on the guest image. Make sure to select
   the "mixed mode" authentication, and the defaults for everything
   else. Select a password, we use `FreeSlick`.
3. When installed, use the manager to enable the TCP protocol on all
   interfaces. This should be as simple as deleting the default `0`
   from the "dynamic port" entry for "all interfaces" and explicitly adding port
   `2008`. You may remove the other protocol options, as we will not
   use them.
4. Add flag `-T8038` to the startup parameters as workaround for
   [virtualbox #3613](https://www.virtualbox.org/ticket/3613) more
   details about [clock timings on the MSDN blog](http://blogs.msdn.com/b/psssql/archive/2009/05/29/how-it-works-sql-server-timings-and-timer-output-gettickcount-timegettime-queryperformancecounter-rdtsc.aspx)
5. Test the JDBC connection from the host by attempting to connect to
   `jdbc:jtds:sqlserver://localhost:2008` with a tool in your
   host system, e.g.
   [Squirrel SQL](http://squirrel-sql.sourceforge.net/) with the
   [jTDS](http://jtds.sourceforge.net/) drivers for MS-SQL.

## Oracle

TODO

## Sybase IQ

TODO

## Sybase ASE

TODO
