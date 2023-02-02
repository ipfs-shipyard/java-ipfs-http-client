#! /bin/sh
wget https://dist.ipfs.io/kubo/v0.18.1/kubo_v0.18.1_darwin-arm64.tar.gz -O /tmp/kubo_darwin-arm64.tar.gz
tar -xvf /tmp/kubo_darwin-arm64.tar.gz
export PATH=$PATH:$PWD/kubo/
ipfs init
ipfs daemon --enable-pubsub-experiment --routing=dhtclient &
