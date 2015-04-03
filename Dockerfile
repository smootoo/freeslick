# https://docs.docker.com/reference/builder/

# docker build -t fommil/freeslick:base .
# docker push fommil/freeslick:base

# This is the base docker image containing the MSSQL VirtualBox VMs.
# It is kept separated so that any changes to the final docker image
# do not require the 5GB MSSQL image to be uploaded every time.

# matching my VPS machine...
#FROM debian:jessie
FROM ubuntu:trusty

MAINTAINER Sam Halliday, sam.halliday@gmail.com

# no point in extracting because:
# 1. https://github.com/docker/docker/issues/12053
# 2. http://stackoverflow.com/questions/29415422
COPY MSSQL.tar.gz /root/VirtualBox/

#ADD MSSQL.tar.gz /root/VirtualBox/
#ADD MSSQL/MSSQL.vbox /root/VirtualBox/MSSQL/MSSQL.vbox
#ADD MSSQL/pcbios.bin /root/VirtualBox/MSSQL/pcbios.bin
#ADD MSSQL/VirtualXP.vdi.bz2 /root/VirtualBox/MSSQL/VirtualXP.vdi.bz2

#RUN sed -i 's|/home/fommil|/root|' /root/VirtualBox/MSSQL/MSSQL.vbox
