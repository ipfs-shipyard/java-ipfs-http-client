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

//    @org.junit.Test
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
            byte[] getResult = ipfs.get(merkleObject);
            if (!Arrays.equals(catResult, file.getContents()))
                throw new IllegalStateException("Different contents!");
            List<MerkleNode> pinRm = ipfs.pin.rm(merkleObject, true);
            if (!pinRm.get(0).equals(merkleObject))
                throw new IllegalStateException("Didn't remove file!");
            Object gc = ipfs.repo.gc();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @org.junit.Test
    public void refsTest() {
        try {
            List<String> local = ipfs.refs.local();
            for (String ref: local) {
                Object refs = ipfs.refs(ref, false);
                if (refs != null)
                    System.out.println(refs);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @org.junit.Test
    public void objectTest() {
        try {
            MerkleNode pointer = new MerkleNode("QmPZ9gcCEpqKTo6aq61g2nXGUhM4iCL3ewB6LDXZCtioEB");
            MerkleNode object = ipfs.object.get(pointer);
            List<MerkleNode> newPointer = ipfs.object.put(Arrays.asList(object.toJSONString().getBytes()));
            MerkleNode links = ipfs.object.links(pointer);
            byte[] data = ipfs.object.data(pointer);
            Map stat = ipfs.object.stat(pointer);
            System.out.println(object);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @org.junit.Test
    public void blockTest() {
        try {
            MerkleNode pointer = new MerkleNode("QmPZ9gcCEpqKTo6aq61g2nXGUhM4iCL3ewB6LDXZCtioEB");
            byte[] object = ipfs.block.get(pointer);
            List<MerkleNode> newPointer = ipfs.block.put(Arrays.asList("Some random data...".getBytes()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @org.junit.Test
    public void swarmTest() {
        try {
            Map bootstrap = ipfs.bootstrap();
            Map<String, Object> addrs = ipfs.swarm.addrs();
            if (addrs.size() > 0) {
                Map id = ipfs.id(addrs.keySet().stream().findAny().get());
                Map ping = ipfs.ping(addrs.keySet().stream().findAny().get());
                System.out.println(ping);
            }
            List<NodeAddress> peers = ipfs.swarm.peers();
            System.out.println(peers);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @org.junit.Test
    public void diagTest() {
        try {
            Map config = ipfs.config.show();
            String val = ipfs.config.get("Datastore.Path");
            Map setResult = ipfs.config.set("Datastore.Path", val);
            ipfs.config.replace(new NamedStreamable.ByteArrayWrapper(JSONParser.toString(config).getBytes()));
//            Object log = ipfs.log();
            String net = ipfs.diag.net();
            System.out.println(net);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @org.junit.Test
    public void toolsTest() {
        try {
            String version = ipfs.version();
            System.out.println(version);
            Map commands = ipfs.commands();
            System.out.println(commands);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // this api is disbaled until deployment over IPFS is enabled
    public void updateTest() {
        try {
            Object check = ipfs.update.check();
            Object update = ipfs.update();
            System.out.println(update);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
