#!/bin/bash

VBoxManage setproperty machinefolder /root/VirtualBox
VBoxManage import /root/MSSQL.ovf
VBoxManage setextradata MSSQL VBoxInternal/Devices/pcbios/0/Config/BiosRom /root/mssql/pcbios.bin

#cd /root/VirtualBox
#tar xf MSSQL.tar.gz
#sed -i 's|/dev/shm|/root|' /root/VirtualBox/MSSQL/MSSQL.vbox
#VBoxManage registervm /root/VirtualBox/MSSQL/MSSQL.vbox 
#VBoxManage snapshot MSSQL restore "MSSQL Up"

VBoxManage startvm --type headless MSSQL

# jdbc:jtds:sqlserver://localhost:2000
# username: sa
# password: FreeSlick

# VBoxManage controlvm MSSQL acpipowerbutton
