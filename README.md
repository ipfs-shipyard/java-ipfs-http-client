# java-ipfs-http-client

[![](https://img.shields.io/badge/made%20by-Protocol%20Labs-blue.svg?style=flat-square)](http://ipn.io)
[![](https://img.shields.io/badge/project-IPFS-blue.svg?style=flat-square)](http://ipfs.io/)
[![](https://img.shields.io/badge/freenode-%23ipfs-blue.svg?style=flat-square)](http://webchat.freenode.net/?channels=%23ipfs)
[![standard-readme compliant](https://img.shields.io/badge/standard--readme-OK-green.svg?style=flat-square)](https://github.com/RichardLitt/standard-readme)

![](https://ipfs.io/ipfs/QmQJ68PFMDdAsgCZvA1UVzzn18asVcf7HVvCDgpjiSCAse)

> A Java client for the IPFS http api

## Table of Contents

- [Install](#install)
- [Usage](#usage)
- [Building](#building)
- [Contribute](#contribute)
- [License](#license)

## Install

### Official releases

You can use this project by including `ipfs.jar` from one of the [releases](https://github.com/ipfs/java-ipfs-api/releases) along with the dependencies.

### Maven, Gradle, SBT

Package managers are supported through [JitPack](https://jitpack.io/#ipfs/java-ipfs-http-client/) which supports Maven, Gradle, SBT, etc.

for Maven, add the following sections to your pom.xml (replacing $LATEST_VERSION):
```
  <repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
  </repositories>

  <dependencies>
    <dependency>
      <groupId>com.github.ipfs</groupId>
      <artifactId>java-ipfs-http-client</artifactId>
      <version>$LATEST_VERSION</version>
    </dependency>
  </dependencies>
```

### Building

Build time requirements:
* Java 21
* Maven 3.9.6+

* Clone this repository
* Run `./mvnw install`
* Copy `target/ipfs-$VERSION.jar` into your project. Appropriate versions of other [dependencies](#dependencies) are also included in `dist/lib/`.
* To copy the project and all dependency JARs to directory, execute `./mvnw eu.maveniverse.maven.plugins:toolbox:gav-copy -DsourceSpec="resolveTransitive(gav(com.github.ipfs:java-ipfs-http-client:v1.4.5-SNAPSHOT))" -DsinkSpec="flat(.)"` in that given directory.
* Run tests using `./mvnw test` (invocation above will run them as well).

### Running tests

To run tests, IPFS daemon must be running on the `127.0.0.1` interface, with `--enable-pubsub-experiment`. 

### IPFS installation

#### Command line

Download ipfs from https://dist.ipfs.io/#go-ipfs and run with `ipfs daemon --enable-pubsub-experiment`

#### Docker Compose

Run `docker-compose up` from the project's root directory. Check [docker-compose.yml](docker-compose.yml) for more details.

## Usage

Create an IPFS instance with:
```Java
IPFS ipfs = new IPFS("/ip4/127.0.0.1/tcp/5001");
```

Then run commands like:
```Java
ipfs.refs.local();
```

To add a file use (the add method returns a list of merklenodes, in this case there is only one element):
```Java
NamedStreamable.FileWrapper file = new NamedStreamable.FileWrapper(new File("hello.txt"));
MerkleNode addResult = ipfs.add(file).get(0);
```

To add a byte[] use:
```Java
NamedStreamable.ByteArrayWrapper file = new NamedStreamable.ByteArrayWrapper("hello.txt", "G'day world! IPFS rocks!".getBytes());
MerkleNode addResult = ipfs.add(file).get(0);
```

To get a file use:
```Java
Multihash filePointer = Multihash.fromBase58("QmPZ9gcCEpqKTo6aq61g2nXGUhM4iCL3ewB6LDXZCtioEB");
byte[] fileContents = ipfs.cat(filePointer);
```

More example usage found [here](./src/main/java/io/ipfs/api/demo)

## Dependencies

Run time requirements:
* Java 11+

Current versions of dependencies are listed in the `pom.xml`, their corresponding source repositories are:

* [multibase](https://github.com/multiformats/java-multibase)
* [multiaddr](https://github.com/multiformats/java-multiaddr)
* [multihash](https://github.com/multiformats/java-multihash)
* [cid](https://github.com/ipld/java-cid)

## Releasing
The version number is specified in `pom.xml` and must be changed in order to be accurately reflected in the JAR file manifest. A git tag must be added in the format `vx.x.x` for [JitPack](https://jitpack.io/#ipfs/java-ipfs-http-client/) to work.

## Contribute

Feel free to join in. All welcome. Open an [issue](https://github.com/ipfs/java-ipfs-api/issues)!

This repository falls under the IPFS [Code of Conduct](https://github.com/ipfs/community/blob/master/code-of-conduct.md).

[![](https://cdn.rawgit.com/jbenet/contribute-ipfs-gif/master/img/contribute.gif)](https://github.com/ipfs/community/blob/master/CONTRIBUTING.md)

## License

[MIT](LICENSE)
