# https://docs.docker.com/reference/builder/

# docker build -t fommil/freeslick:latest .
# docker push fommil/freeslick:latest
# docker run -i -t --device=/dev/vboxdrv fommil/freeslick:latest

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
