#! /bin/sh
wget https://dist.ipfs.io/go-ipfs/v0.5.0/go-ipfs_v0.5.0_linux-amd64.tar.gz -O /tmp/go-ipfs_linux-amd64.tar.gz
tar -xvf /tmp/go-ipfs_linux-amd64.tar.gz
export PATH=$PATH:$PWD/go-ipfs/
ipfs init
ipfs daemon --enable-pubsub-experiment &
