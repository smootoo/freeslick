# https://docs.docker.com/reference/builder/

# docker build --no-cache -t fommil/freeslick:latest .
# docker push fommil/freeslick:latest

# we're currently expecting the host to be running the MSSQL
# instances, which means the CI job can be started by running, e.g.
#
#  docker run -it --rm --net host  fommil/freeslick /root/ci.sh fommil master
#
# but we would certainly like to move towards starting MSSQL inside the container, e.g.
#
#  docker run -it --rm --device=/dev/vboxdrv fommil/freeslick /root/ci.sh fommil master
#
#  or (at least)
#
#  docker run -it --rm --privileged=true fommil/freeslick /root/ci.sh fommil master
#
# unfortunately VirtualBox is very flakey when running inside docker.
# It seems to want the host/guest to have *exactly* the same OS and
# virtualbox version.

FROM fommil/freeslick:build

MAINTAINER Sam Halliday, sam.halliday@gmail.com

################################################
# FreeSlick Ivy Cache
# (could do with using the right scala versions)
RUN\
  cd /root &&\
  git clone https://github.com/fommil/freeslick.git &&\
  cd freeslick &&\
  for BRANCH in master ; do\
    git reset --hard origin/$BRANCH &&\
    git clean -xfd &&\
    sbt updateClassifiers doc ;\
  done &&\
  rm -rf /root/freeslick
