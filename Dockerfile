# https://docs.docker.com/reference/builder/
# NOTE dockerignore is ignored https://github.com/docker/docker/issues/9455

FROM debian:jessie

MAINTAINER Sam Halliday, sam.halliday@gmail.com

# We have to use Oracle's JDK 6 because Jessie does not have OpenJDK 6
# (and we don't want to restrict FreeSlick to JDK7+)
ENV JAVA_VARIANT java-6-oracle
ENV JAVA_HOME /usr/lib/jvm/${JAVA_VARIANT}/jre/
ENV JDK_HOME /usr/lib/jvm/${JAVA_VARIANT}/
ENV SBT_VARIANTS 0.13.7
ENV SCALA_VARIANTS 2.10.5 2.11.6

################################################
# Package Management
RUN\
  echo 'deb http://ppa.launchpad.net/webupd8team/java/ubuntu precise main' >> /etc/apt/sources.list ;\
  echo 'deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu precise main' >> /etc/apt/sources.list ;\
  apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys EEA14886 ;\
  cat /etc/apt/sources.list | sed 's/^deb /deb-src /' >> /etc/apt/sources.list ;\
  echo 'APT::Install-Recommends "0";' >> /etc/apt/apt.conf ;\
  echo 'APT::Install-Suggests "0";' >> /etc/apt/apt.conf ;\
  apt-get update -qq ;\
  apt-get upgrade -qq ;\
  apt-get autoremove -qq ;\
  apt-get clean


################################################
# Java
#
# Alternative Javas by inspired by https://github.com/dockerfile/java/
RUN\
  for V in 6 ; do\
    echo oracle-java${V}-installer shared/accepted-oracle-license-v1-1 select true | debconf-set-selections ;\
  done ;\
  apt-get install -y oracle-java6-installer\
  update-java-alternatives -s ${JAVA_VARIANT} ;\
  apt-get clean

################################################
# SBT (and by implication, Scala)
ADD https://raw.githubusercontent.com/paulp/sbt-extras/master/sbt /usr/bin/sbt
RUN chmod a+x /usr/bin/sbt
RUN\
  apt-get install -qq curl ;\
  apt-get clean ;\
  mkdir /tmp/sbt ;\
  cd /tmp/sbt ;\
  mkdir -p project src/main/scala ;\
  touch src/main/scala/scratch.scala ;\
  for SBT_VERSION in $SBT_VARIANTS ; do\
    echo "sbt.version=$SBT_VERSION" > project/build.properties ;\
    for SCALA_VERSION in $SCALA_VARIANTS ; do\
      sbt ++$SCALA_VERSION clean updateClassifiers compile ;\
    done ;\
  done ;\
  rm -rf /tmp/sbt

################################################
# VirtualBox
RUN\
  apt-get install -y virtualbox ;\
  apt-get clean
  
################################################
# Shippable
#
# Shippable tries to install these scripts on startup, so we may as
# well add them here since it is our CI platform.
RUN\
  apt-get install -y git-core python-pip ssh ;\
  apt-get clean ;\
  pip install virtualenv ;\
  pip install --install-option="--prefix=/root/ve" pika boto glob2 ;\
  echo "en_US.UTF-8 UTF-8" >> /etc/locale.gen ;\
  locale-gen
