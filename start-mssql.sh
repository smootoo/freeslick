#!/bin/bash

# even if this was uncompressed, docker needs to do copy-on-write
bzip2 -cd VirtualBox/MSSQL/VirtualXP.vdi.bz2 > VirtualBox/MSSQL/VirtualXP.vdi

#VBoxManage setproperty machinefolder /root/VirtualBox
#VBoxManage registervm /root/VirtualBox/MSSQL/MSSQL.vbox 
VBoxManage startvm --type headless MSSQL

# jdbc:jtds:sqlserver://localhost:2000
# username: sa
# password: FreeSlick

# VBoxManage controlvm MSSQL acpipowerbutton
