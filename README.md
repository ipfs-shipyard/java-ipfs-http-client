# java-ipfs-api

[![](https://img.shields.io/badge/made%20by-Protocol%20Labs-blue.svg?style=flat-square)](http://ipn.io)
[![](https://img.shields.io/badge/project-IPFS-blue.svg?style=flat-square)](http://ipfs.io/)
[![](https://img.shields.io/badge/freenode-%23ipfs-blue.svg?style=flat-square)](http://webchat.freenode.net/?channels=%23ipfs)
[![standard-readme compliant](https://img.shields.io/badge/standard--readme-OK-green.svg?style=flat-square)](https://github.com/RichardLitt/standard-readme)
[![FOSSA Status](https://app.fossa.io/api/projects/git%2Bhttps%3A%2F%2Fgithub.com%2Fipfs%2Fjava-ipfs-api.svg?type=shield)](https://app.fossa.io/projects/git%2Bhttps%3A%2F%2Fgithub.com%2Fipfs%2Fjava-ipfs-api?ref=badge_shield)

> A Java implementation of the IPFS http api

## Table of Contents

- [Install](#install)
- [Usage](#usage)
- [Building](#building)
- [Contribute](#contribute)
- [License](#license)

## Install

Simply clone this repo.

## Usage

Include the IPFS.jar in your project.

Create an IPFS instance with:
```Java
IPFS ipfs = new IPFS("/ip4/127.0.0.1/tcp/5001");
```

Then run commands like:
```Java
ipfs.refs.local();
```

To add a file use:
```Java
NamedStreamable.FileWrapper file = new NamedStreamable.FileWrapper(new File("hello.txt"));
MerkleNode addResult = ipfs.add(file);
```

To add a byte[] use:
```Java
NamedStreamable.ByteArrayWrapper file = new NamedStreamable.ByteArrayWrapper("hello.txt", "G'day world! IPFS rocks!".getBytes());
MerkleNode addResult = ipfs.add(file);
```

To get a file use:
```Java
Multihash filePointer = Multihash.fromBase58("QmPZ9gcCEpqKTo6aq61g2nXGUhM4iCL3ewB6LDXZCtioEB");
byte[] fileContents = ipfs.cat(filePointer);
```

## Dependencies
* [multibase](https://github.com/multiformats/java-multibase)
* [multiaddr](https://github.com/multiformats/java-multiaddr)
* [multihash](https://github.com/multiformats/java-multihash)
* [cid](https://github.com/ipld/java-cid)

## Building
---------
To build just run `ant dist`. The dependencies are included in the `./lib` directory, and automatically copied to the `./dist/lib` directory.

To run tests use `ant test`.

## Contribute

Feel free to join in. All welcome. Open an [issue](https://github.com/ipfs/java-ipfs-api/issues)!

This repository falls under the IPFS [Code of Conduct](https://github.com/ipfs/community/blob/master/code-of-conduct.md).

[![](https://cdn.rawgit.com/jbenet/contribute-ipfs-gif/master/img/contribute.gif)](https://github.com/ipfs/community/blob/master/contributing.md)

## License

[MIT](LICENSE)

[![FOSSA Status](https://app.fossa.io/api/projects/git%2Bhttps%3A%2F%2Fgithub.com%2Fipfs%2Fjava-ipfs-api.svg?type=large)](https://app.fossa.io/projects/git%2Bhttps%3A%2F%2Fgithub.com%2Fipfs%2Fjava-ipfs-api?ref=badge_large)
