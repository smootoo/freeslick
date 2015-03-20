# https://docs.docker.com/reference/builder/

# docker build -t fommil/freeslick:base .
# docker push fommil/freeslick:base

# This is the base docker image containing the MSSQL VirtualBox VMs.
# It is kept separated so that any changes to the final docker image
# do not require the 5GB MSSQL image to be uploaded every time.

FROM debian:jessie

MAINTAINER Sam Halliday, sam.halliday@gmail.com

ADD MSSQL.tar.gz /root/VirtualBox/
RUN sed -i 's|/home/fommil|/root|' /root/VirtualBox/MSSQL/MSSQL.vbox
