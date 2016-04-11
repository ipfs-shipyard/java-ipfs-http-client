# java-ipfs-api
A Java implementation of the IPFS http api

##Usage
-----
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

##Building
---------
To build just run make. There are no dependencies, just include the resulting IPFS.jar in your project. 

To run tests use make tests.
