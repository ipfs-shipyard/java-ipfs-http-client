package org.ipfs.api;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

public class Test {

    IPFS ipfs = new IPFS(new MultiAddress("/ip4/127.0.0.1/tcp/5001"));
    @org.junit.Test
    public void base58Test() {
        String input = "QmPZ9gcCEpqKTo6aq61g2nXGUhM4iCL3ewB6LDXZCtioEB";
        byte[] output = Base58.decode(input);
        String encoded = Base58.encode(output);
        if (!encoded.equals(input))
            throw new IllegalStateException("Incorrect base58! "+ input + " => "+encoded);
    }


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

    @org.junit.Test
    public void hostFileTest() {
        NamedStreamable hostFile = new NamedStreamable.FileWrapper(new File("Makefile"));
        fileTest(hostFile);
    }

    public void fileTest(NamedStreamable file) {
        try {
            MerkleNode addResult = ipfs.add(file);
            List<MerkleNode> lsResult = ipfs.ls(addResult.hash);
            if (lsResult.size() != 1)
                throw new IllegalStateException("Incorrect number of objects in ls!");
            if (!lsResult.get(0).equals(addResult))
                throw new IllegalStateException("Object not returned in ls!");
            byte[] catResult = ipfs.cat(addResult.hash);
            byte[] getResult = ipfs.get(addResult.hash);
            if (!Arrays.equals(catResult, file.getContents()))
                throw new IllegalStateException("Different contents!");
            List<Multihash> pinRm = ipfs.pin.rm(addResult.hash, true);
            if (!pinRm.get(0).equals(addResult.hash))
                throw new IllegalStateException("Didn't remove file!");
            Object gc = ipfs.repo.gc();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @org.junit.Test
    public void pinTest() {
        try {
            MerkleNode file = ipfs.add(new NamedStreamable.ByteArrayWrapper("some data".getBytes()));
            Multihash hash = file.hash;
            Map<Multihash, Object> ls1 = ipfs.pin.ls(IPFS.PinType.all);
            boolean pinned = ls1.containsKey(hash);
            List<Multihash> rm = ipfs.pin.rm(hash);
            // second rm should not throw a http 500, but return an empty list
//            List<Multihash> rm2 = ipfs.pin.rm(hash);
            List<Multihash> add2 = ipfs.pin.add(hash);
            // adding something already pinned should succeed
            List<Multihash> add3 = ipfs.pin.add(hash);
            Map<Multihash, Object> ls = ipfs.pin.ls();
            System.out.println(ls);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @org.junit.Test
    public void objectPatch() {
        try {
            MerkleNode obj = ipfs.object._new(Optional.empty());
            Multihash base = obj.hash;
            // link tests
            String linkName = "alink";
            MerkleNode addLink = ipfs.object.patch(base, "add-link", Optional.empty(), Optional.of(linkName), Optional.of(base));
            MerkleNode withLink = ipfs.object.get(addLink.hash);
            if (withLink.links.size() != 1 || !withLink.links.get(0).hash.equals(base) || !withLink.links.get(0).name.get().equals(linkName))
                throw new RuntimeException("Added link not correct!");
            MerkleNode rmLink = ipfs.object.patch(addLink.hash, "rm-link", Optional.empty(), Optional.of(linkName), Optional.empty());
            if (!rmLink.hash.equals(base))
                throw new RuntimeException("Adding not inverse of removing link!");

            // data tests
            byte[] data = "some random textual data".getBytes();
//            byte[] data = new byte[1024];
//            new Random().nextBytes(data);
            MerkleNode patched = ipfs.object.patch(base, "set-data", Optional.of(data), Optional.empty(), Optional.empty());
            MerkleNode patchedResult = ipfs.object.get(patched.hash);
            if (!Arrays.equals(patchedResult.data.get(), data))
                throw new RuntimeException("object.patch: returned data != stored data!");

            MerkleNode twicePatched = ipfs.object.patch(patched.hash, "append-data", Optional.of(data), Optional.empty(), Optional.empty());
            MerkleNode twicePatchedResult = ipfs.object.get(twicePatched.hash);
            byte[] twice = new byte[2*data.length];
            for (int i=0; i < 2; i++)
                System.arraycopy(data, 0, twice, i*data.length, data.length);
            if (!Arrays.equals(twicePatchedResult.data.get(), twice))
                throw new RuntimeException("object.patch: returned data after append != stored data!");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @org.junit.Test
    public void refsTest() {
        try {
            List<Multihash> local = ipfs.refs.local();
            for (Multihash ref: local) {
                Object refs = ipfs.refs(ref, false);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @org.junit.Test
    public void objectTest() {
        try {
            MerkleNode _new = ipfs.object._new(Optional.empty());
            Multihash pointer = Multihash.fromBase58("QmPZ9gcCEpqKTo6aq61g2nXGUhM4iCL3ewB6LDXZCtioEB");
            MerkleNode object = ipfs.object.get(pointer);
            List<MerkleNode> newPointer = ipfs.object.put(Arrays.asList(object.toJSONString().getBytes()));
            List<MerkleNode> newPointer2 = ipfs.object.put("json", Arrays.asList(object.toJSONString().getBytes()));
            MerkleNode links = ipfs.object.links(pointer);
            byte[] data = ipfs.object.data(pointer);
            Map stat = ipfs.object.stat(pointer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @org.junit.Test
    public void blockTest() {
        try {
            MerkleNode pointer = new MerkleNode("QmPZ9gcCEpqKTo6aq61g2nXGUhM4iCL3ewB6LDXZCtioEB");
            Map stat = ipfs.block.stat(pointer.hash);
            byte[] object = ipfs.block.get(pointer.hash);
            List<MerkleNode> newPointer = ipfs.block.put(Arrays.asList("Some random data...".getBytes()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @org.junit.Test
    public void fileTest() {
        try {
            ipfs.repo.gc();
            List<Multihash> local = ipfs.refs.local();
            for (Multihash hash: local) {
                Map ls = ipfs.file.ls(hash);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @org.junit.Test
    public void nameTest() {
        try {
            MerkleNode pointer = new MerkleNode("QmPZ9gcCEpqKTo6aq61g2nXGUhM4iCL3ewB6LDXZCtioEB");
            Map pub = ipfs.name.publish(pointer.hash);
            String resolved = ipfs.name.resolve(Multihash.fromBase58((String) pub.get("Name")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @org.junit.Test
    public void dnsTest() {
        try {
            String domain = "ipfs.io";
            String dns = ipfs.dns(domain);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void mountTest() {
        try {
            Map mount = ipfs.mount(null, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @org.junit.Test
    public void dhtTest() {
        try {
            Multihash pointer = Multihash.fromBase58("QmPZ9gcCEpqKTo6aq61g2nXGUhM4iCL3ewB6LDXZCtioEB");
            Map get = ipfs.dht.get(pointer);
            Map put = ipfs.dht.put("somekey", "somevalue");
            Map findprovs = ipfs.dht.findprovs(pointer);
            List<MultiAddress> peers = ipfs.swarm.peers();
            Map query = ipfs.dht.query(peers.get(0));
//            Map find = ipfs.dht.findpeer(peers.get(0));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @org.junit.Test
    public void statsTest() {
        try {
            Map stats = ipfs.stats.bw();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void resolveTest() {
        try {
            Multihash hash = Multihash.fromBase58("QmatmE9msSfkKxoffpHwNLNKgwZG8eT9Bud6YoPab52vpy");
            Map res = ipfs.resolve("ipns", hash, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @org.junit.Test
    public void swarmTest() {
        try {
            String multiaddr = "/ip4/104.131.131.82/tcp/4001/ipfs/QmaCpDMGvV2BGHeYERUEnRQAwe3N8SzbUtfsmvsqQLuvuJ";
            Map connect = ipfs.swarm.connect(multiaddr);
            Map disconnect = ipfs.swarm.disconnect(multiaddr);
            Map<String, Object> addrs = ipfs.swarm.addrs();
            if (addrs.size() > 0) {
                String target = addrs.keySet().stream().findAny().get();
                Map id = ipfs.id(target);
                Map ping = ipfs.ping(target);
            }
            List<MultiAddress> peers = ipfs.swarm.peers();
            System.out.println(peers);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @org.junit.Test
    public void bootstrapTest() {
        try {
            List<MultiAddress> bootstrap = ipfs.bootstrap.list();
            System.out.println(bootstrap);
            List<MultiAddress> rm = ipfs.bootstrap.rm(bootstrap.get(0), false);
            List<MultiAddress> add = ipfs.bootstrap.add(bootstrap.get(0));
            System.out.println();
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @org.junit.Test
    public void toolsTest() {
        try {
            String version = ipfs.version();
            Map commands = ipfs.commands();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // this api is disbaled until deployment over IPFS is enabled
    public void updateTest() {
        try {
            Object check = ipfs.update.check();
            Object update = ipfs.update();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
