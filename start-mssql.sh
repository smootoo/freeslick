#!/bin/bash

#VBoxManage setproperty machinefolder /root/VirtualBox
#VBoxManage registervm /root/VirtualBox/MSSQL/MSSQL.vbox 
VBoxManage startvm --type headless MSSQL

while ! nc -z localhost 2008; do sleep 1; done && echo 'SQL2008 is starting'
while ! nc -z localhost 2005; do sleep 1; done && echo 'SQL2005 is starting'
while ! nc -z localhost 2000; do sleep 1; done && echo 'SQL2000 is starting'

export TDSVER=7.0
echo quit | tsql -H localhost -p 2008 -U sa -P FreeSlick && echo 'SQL2008 is up'
echo quit | tsql -H localhost -p 2005 -U sa -P FreeSlick && echo 'SQL2005 is up'
echo quit | tsql -H localhost -p 2000 -U sa -P FreeSlick && echo 'SQL2000 is up'

# jdbc:jtds:sqlserver://localhost:2000
# username: sa
# password: FreeSlick

# VBoxManage controlvm MSSQL acpipowerbutton
