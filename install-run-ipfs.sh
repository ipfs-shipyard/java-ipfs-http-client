#! /bin/sh
wget https://dist.ipfs.io/kubo/v0.17.0/kubo_v0.17.0_linux-amd64.tar.gz -O /tmp/kubo_linux-amd64.tar.gz
tar -xvf /tmp/kubo_linux-amd64.tar.gz
export PATH=$PATH:$PWD/kubo/
ipfs init
ipfs daemon --enable-pubsub-experiment --routing=dhtclient &
