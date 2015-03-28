#!/bin/bash

# http://www.freetds.org/userguide/serverthere.htm
# http://www.freetds.org/userguide/choosingtdsprotocol.htm
# must set 'timeout' to something sensible in ~/.freetds.conf
function pingmssqls {
    echo "Attempting to connect to MSSQL servers..."
    declare -i RESULT=0
    echo quit | TDSVER=7.3 tsql -o q -H localhost -p 2008 -U sa -P FreeSlick 2> /dev/null && echo "MSSQL 2008 is up"
    RESULT+=$?
    echo quit | TDSVER=7.2 tsql -o q -H localhost -p 2005 -U sa -P FreeSlick 2> /dev/null && echo "MSSQL 2005 is up"
    RESULT+=$?
    echo quit | TDSVER=7.1 tsql -o q -H localhost -p 2000 -U sa -P FreeSlick 2> /dev/null && echo "MSSQL 2000 is up"
    RESULT+=$?
    return $RESULT
}
while ! pingmssqls ; do sleep 1 ; done && echo 'MSSQL are up'
