FROM ubuntu:focal

ADD package /tmp/package
RUN apt-get update -y && apt -y install wget libssl1.1
WORKDIR /tmp/package
RUN dpkg -i odyssey_1.3-200_amd64.deb
RUN addgroup odyssey && \
    useradd odyssey -g odyssey
USER odyssey:odyssey
WORKDIR /home/odyssey
RUN wget https://raw.githubusercontent.com/dimarudik/odyssey-prepared/odyssey-pps-workload/k8s/odyssey-prepared/odyssey.conf

ENTRYPOINT ["odyssey","/home/odyssey/odyssey.conf"]
#docker build . -t dimarudik/odyssey
