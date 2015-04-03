# https://docs.docker.com/reference/builder/

# docker build --no-cache -t fommil/freeslick:build .
# docker push fommil/freeslick:build

# We're currently expecting the host to be running the MSSQL
# instances, which means the CI job can be started by running, e.g.
#
#  docker run -it --rm --net host fommil/freeslick:build /root/ci.sh fommil master
#
# but we would certainly like to move towards starting MSSQL inside the container, e.g.
#
#  docker run -it --rm --device=/dev/vboxdrv fommil/freeslick:build /root/ci.sh fommil master
#
#  or (at least)
#
#  docker run -it --rm --privileged=true fommil/freeslick:build /root/ci.sh fommil master
#
# unfortunately VirtualBox is very flakey when running inside docker.
# It seems to want the host/guest to have *exactly* the same OS and
# virtualbox version.

FROM fommil/freeslick:base

MAINTAINER Sam Halliday, sam.halliday@gmail.com

# We have to use Oracle's JDK 6 because Jessie does not have OpenJDK 6
# (and we don't want to restrict FreeSlick to JDK7+)
ENV JAVA_VARIANT java-6-openjdk-amd64
ENV JAVA_HOME /usr/lib/jvm/${JAVA_VARIANT}/jre/
ENV JDK_HOME /usr/lib/jvm/${JAVA_VARIANT}/
ENV SBT_VARIANTS 0.13.8
ENV SCALA_VARIANTS 2.10.5 2.11.6

################################################
# Package Management
ADD https://www.virtualbox.org/download/oracle_vbox.asc /etc/oracle_vbox.asc
RUN\
  apt-key add /etc/oracle_vbox.asc &&\
  echo 'deb http://download.virtualbox.org/virtualbox/debian trusty contrib' >> /etc/apt/sources.list &&\
  echo 'APT::Install-Recommends "0";' >> /etc/apt/apt.conf &&\
  echo 'APT::Install-Suggests "0";' >> /etc/apt/apt.conf &&\
  apt-get update -qq &&\
  apt-get autoremove -qq &&\
  apt-get clean


################################################
# Java
#
# Alternative Javas by inspired by https://github.com/dockerfile/java/
RUN\
  apt-get install -y openjdk-6-jdk &&\
  apt-get clean

################################################
# SBT (and by implication, Scala)
#ADD https://raw.githubusercontent.com/paulp/sbt-extras/master/sbt /usr/bin/sbt
ADD sbt /usr/bin/sbt
RUN\
  chmod a+x /usr/bin/sbt &&\
  apt-get install -qq curl &&\
  apt-get clean &&\
  mkdir /tmp/sbt &&\
  cd /tmp/sbt &&\
  mkdir -p project src/main/scala &&\
  touch src/main/scala/scratch.scala &&\
  for SBT_VERSION in $SBT_VARIANTS ; do\
    echo "sbt.version=$SBT_VERSION" > project/build.properties &&\
    for SCALA_VERSION in $SCALA_VARIANTS ; do\
      sbt ++$SCALA_VERSION clean updateClassifiers compile ;\
    done ;\
  done &&\
  rm -rf /tmp/sbt

################################################
# VirtualBox and MSSQL
#
# WORKAROUND https://github.com/docker/docker/issues/8318 virtualbox
# really expects images in "/root/VirtualBox VMs" but spaces are not
# allowed in ADD commands. Also, we need to fake /dev/vboxdrv[u] to be
# able to use VBoxManage.
ADD start-mssql.sh /root/start-mssql.sh
ADD await-mssql.sh /root/await-mssql.sh
ADD .freetds.conf /root/.freetds.conf
RUN\
  apt-get install -qq virtualbox-4.3 kmod freetds-bin bzip2 &&\
  apt-get clean &&\
  chmod a+x /root/start-mssql.sh /root/await-mssql.sh

################################################
# Shippable
#
# Shippable tries to install these scripts on startup, so we may as
# well add them here since it is our CI platform.
RUN\
  apt-get install -qq locales git-core python-pip ssh &&\
  apt-get clean &&\
  pip install virtualenv &&\
  pip install --install-option="--prefix=/root/ve" pika boto glob2 &&\
  echo "en_US.UTF-8 UTF-8" >> /etc/locale.gen &&\
  locale-gen

################################################
# CI hack until we have dedicated hardware
ADD ci.sh /root/ci.sh
RUN chmod a+x /root/ci.sh
