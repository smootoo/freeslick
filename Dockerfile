# https://docs.docker.com/reference/builder/

# docker build --no-cache -t fommil/freeslick:build .
# docker push fommil/freeslick:build
# docker run -i -t --device=/dev/vboxdrv fommil/freeslick:build

# NOTE: in order to run this docker container, the Host must have the
#       VirtualBox version 4.3.18 (or later) DKMS modules loaded.

FROM fommil/freeslick:base

MAINTAINER Sam Halliday, sam.halliday@gmail.com

# We have to use Oracle's JDK 6 because Jessie does not have OpenJDK 6
# (and we don't want to restrict FreeSlick to JDK7+)
ENV JAVA_VARIANT java-6-oracle
ENV JAVA_HOME /usr/lib/jvm/${JAVA_VARIANT}/jre/
ENV JDK_HOME /usr/lib/jvm/${JAVA_VARIANT}/
ENV SBT_VARIANTS 0.13.8
ENV SCALA_VARIANTS 2.10.5 2.11.6

################################################
# Package Management
RUN\
  echo 'deb http://ppa.launchpad.net/webupd8team/java/ubuntu precise main' >> /etc/apt/sources.list &&\
  echo 'deb http://http.debian.net/debian jessie contrib' >> /etc/apt/sources.list &&\
  apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys EEA14886 &&\
  cat /etc/apt/sources.list | sed 's/^deb /deb-src /' >> /etc/apt/sources.list &&\
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
  for V in 6 ; do\
    echo oracle-java${V}-installer shared/accepted-oracle-license-v1-1 select true | debconf-set-selections ;\
  done &&\
  apt-get install -y oracle-java6-installer &&\
  update-java-alternatives -s ${JAVA_VARIANT} &&\
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
  apt-get install -qq virtualbox netcat freetds-bin &&\
  apt-get clean &&\
  ln -s zero /dev/vboxdrv &&\
  ln -s zero /dev/vboxdrvu &&\
  VBoxManage setproperty machinefolder /root/VirtualBox &&\
  chmod a+x /root/start-mssql.sh /root/await-mssql.sh &&\
  VBoxManage registervm /root/VirtualBox/MSSQL/MSSQL.vbox &&\
  rm /dev/vboxdrv /dev/vboxdrvu

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
