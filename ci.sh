#!/bin/bash

# We need dedicated hardware to be able to do our CI in shippable,
# because we need to use VirtualBox to start a Windows XP instance
# containing MSSQL... but our dedicated hosts are only poor VPS
# machines that can't nested virtual machines. Therefore,
# contributors/admins must manually run the CI job for pull requests.

# This script should be called with two parameters:
#
# 1. github username
# 2. freeslick branch name

USERNAME=$1
BRANCH=$1

if [ -n "$USERNAME" ] ; then
    echo "first parameter must be a github username"
    exit 1
fi
if [ -n "$BRANCH" ] ; then
    echo "second parameter must be a freeslick branch name"
    exit 1
fi

cd /root &&\
/root/start-mssql.sh &&\
git clone -b $BRANCH --single-branch https://github.com/$USERNAME/freeslick.git &&\
cd freeslick &&\
sbt update &&\
sbt test:compile it:compile doc &&\
if [ $(git diff | wc -l) -ge 1 ] ; then
  echo "Code formatting does not meet the project's standards:" ;
  git --no-pager diff ;
  exit 1 ;
fi &&\
sbt test &&\
/root/await-mssql.sh &&\
sbt it:test
