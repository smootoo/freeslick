#!/bin/bash

#VBoxManage setproperty machinefolder /root/VirtualBox
#VBoxManage registervm /root/VirtualBox/MSSQL/MSSQL.vbox 
VBoxManage startvm --type headless MSSQL

while ! nc -z localhost 2008; do sleep 1; done && echo 'SQL2008 is starting'
while ! nc -z localhost 2005; do sleep 1; done && echo 'SQL2005 is starting'
while ! nc -z localhost 2000; do sleep 1; done && echo 'SQL2000 is starting'

# http://www.freetds.org/userguide/serverthere.htm
# http://www.freetds.org/userguide/choosingtdsprotocol.htm
echo quit | TDSVER=7.3 tsql -H localhost -p 2008 -U sa -P FreeSlick && echo 'SQL2008 is up'
echo quit | TDSVER=7.2 tsql -H localhost -p 2005 -U sa -P FreeSlick && echo 'SQL2005 is up'
echo quit | TDSVER=7.1 tsql -H localhost -p 2000 -U sa -P FreeSlick && echo 'SQL2000 is up'

# jdbc:jtds:sqlserver://localhost:2000
# username: sa
# password: FreeSlick

# VBoxManage controlvm MSSQL acpipowerbutton
