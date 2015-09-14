package org.ipfs;

import java.io.*;
import java.util.*;

public class Test {

    IPFS ipfs = new IPFS("127.0.0.1", 5001);

    @org.junit.Test
    public void singleFileTest() {
        NamedStreamable.ByteArrayWrapper file = new NamedStreamable.ByteArrayWrapper("hello.txt", "G'day world! IPFS rocks!".getBytes());
        fileTest(file);
    }

    @org.junit.Test
    public void largeFileTest() {
        byte[] largerData = new byte[100*1024*1024];
        new Random(1).nextBytes(largerData);
        NamedStreamable.ByteArrayWrapper largeFile = new NamedStreamable.ByteArrayWrapper("nontrivial.txt", largerData);
        fileTest(largeFile);
    }

    public void fileTest(NamedStreamable file) {
        try {
            List<NamedStreamable> inputFiles = Arrays.asList(file);
            List<MerkleNode> addResult = ipfs.add(inputFiles);
            MerkleNode merkleObject = addResult.get(0);
            List<MerkleNode> lsResult = ipfs.ls(merkleObject);
            if (lsResult.size() != 1)
                throw new IllegalStateException("Incorrect number of objects in ls!");
            if (!lsResult.get(0).equals(merkleObject))
                throw new IllegalStateException("Object not returned in ls!");
            byte[] catResult = ipfs.cat(merkleObject);
            if (!Arrays.equals(catResult, file.getContents()))
                throw new IllegalStateException("Different contents!");
            List<MerkleNode> pinRm = ipfs.pin.rm(merkleObject, true);
            if (!pinRm.get(0).equals(merkleObject))
                throw new IllegalStateException("Didn't remove file!");
            Object gc = ipfs.repo.gc();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @org.junit.Test
    public void objectTest() {
        try {
            List<MerkleNode> newPointer = ipfs.object.put(Arrays.asList("Some random data".getBytes()));
            MerkleNode pointer = new MerkleNode("QmPZ9gcCEpqKTo6aq61g2nXGUhM4iCL3ewB6LDXZCtioEB");
            MerkleNode object = ipfs.object.get(pointer);
            MerkleNode links = ipfs.object.links(pointer);
            byte[] data = ipfs.object.data(pointer);
            Map stat = ipfs.object.stat(pointer);
            System.out.println(object);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
