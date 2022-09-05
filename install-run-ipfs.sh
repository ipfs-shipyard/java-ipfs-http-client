#! /bin/sh
wget https://dist.ipfs.io/kubo/v0.15.0/kubo_v0.15.0_linux-amd64.tar.gz -O /tmp/go-ipfs_linux-amd64.tar.gz
tar -xvf /tmp/go-ipfs_linux-amd64.tar.gz
export PATH=$PATH:$PWD/go-ipfs/
ipfs init
ipfs daemon --enable-pubsub-experiment --routing=dhtclient &
