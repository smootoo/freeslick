#!/bin/bash

#VBoxManage setproperty machinefolder /root/VirtualBox
#VBoxManage registervm /root/VirtualBox/MSSQL/MSSQL.vbox 
VBoxManage snapshot MSSQL restore "MSSQL Up"
VBoxManage startvm --type headless MSSQL

# jdbc:jtds:sqlserver://localhost:2000
# username: sa
# password: FreeSlick

# VBoxManage controlvm MSSQL acpipowerbutton
