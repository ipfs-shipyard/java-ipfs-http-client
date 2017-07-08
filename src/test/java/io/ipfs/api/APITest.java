package io.ipfs.api;

import io.ipfs.api.cbor.*;
import io.ipfs.cid.*;
import io.ipfs.multihash.Multihash;
import io.ipfs.multiaddr.MultiAddress;
import org.junit.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static org.junit.Assert.assertTrue;

public class APITest {

    private final IPFS ipfs = new IPFS(new MultiAddress("/ip4/127.0.0.1/tcp/5001"));

    @org.junit.Test
    public void dag() throws IOException {
        byte[] object = "{\"data\":1234}".getBytes();
        MerkleNode put = ipfs.dag.put("json", object);

        Cid expected = Cid.decode("zdpuB2CbdLrUK5AgZusm4hraisDDDC135ugdmZWvMHhhsSYTb");

        Multihash result = put.hash;
        Assert.assertTrue("Correct cid returned", result.equals(expected));

        byte[] get = ipfs.dag.get(expected);
        Assert.assertTrue("Raw data equal", Arrays.equals(object, get));
    }

    @org.junit.Test
    public void dagCbor() throws IOException {
        Map<String, CborObject> tmp = new TreeMap<>();
        tmp.put("data", new CborObject.CborByteArray("G'day mate!".getBytes()));
        CborObject original = CborObject.CborMap.build(tmp);
        byte[] object = original.toByteArray();
        MerkleNode put = ipfs.dag.put("cbor", object);

        Cid cid = (Cid) put.hash;

        byte[] get = ipfs.dag.get(cid);
        CborObject cborObject = CborObject.fromByteArray(get);
        Assert.assertTrue("Raw data equal", Arrays.equals(object, get));

        Cid expected = Cid.decode("zdpuB2RwxeC5eC7oxiyzzhuZwAPd26YNRxXHvcTvgm4MbXwsC");
        Assert.assertTrue("Correct cid returned", cid.equals(expected));
    }

    @Test
    public void keys() throws IOException {
        List<KeyInfo> existing = ipfs.key.list();
        String name = "mykey" + System.nanoTime();
        KeyInfo gen = ipfs.key.gen(name, Optional.of("rsa"), Optional.of("2048"));
        String newName = "bob" + System.nanoTime();
        Object rename = ipfs.key.rename(name, newName);
        List<KeyInfo> rm = ipfs.key.rm(newName);
        List<KeyInfo> remaining = ipfs.key.list();
        Assert.assertTrue("removed key", remaining.equals(existing));
    }

    @Test
    public void ipldNode() {
        Function<Stream<Pair<String, CborObject>>, CborObject.CborMap> map =
                s -> CborObject.CborMap.build(s.collect(Collectors.toMap(p -> p.left, p -> p.right)));
        CborObject.CborMap a = map.apply(Stream.of(new Pair<>("b", new CborObject.CborLong(1))));

        CborObject.CborMap cbor = map.apply(Stream.of(new Pair<>("a", a), new Pair<>("c", new CborObject.CborLong(2))));

        IpldNode.CborIpldNode node = new IpldNode.CborIpldNode(cbor);
        List<String> tree = node.tree("", -1);
        Assert.assertTrue("Correct tree", tree.equals(Arrays.asList("/a/b", "/c")));
    }

    @org.junit.Test
    public void singleFileTest() {
        NamedStreamable.ByteArrayWrapper file = new NamedStreamable.ByteArrayWrapper("hello.txt", "G'day world! IPFS rocks!".getBytes());
        fileTest(file);
    }

    @org.junit.Test
    public void dirTest() throws IOException {
        NamedStreamable.DirWrapper dir = new NamedStreamable.DirWrapper("root", Arrays.asList());
        MerkleNode addResult = ipfs.add(dir);
        List<MerkleNode> ls = ipfs.ls(addResult.hash);
        Assert.assertTrue(ls.size() > 0);
    }

    @org.junit.Test
    public void directoryTest() throws IOException {
        Random rnd = new Random();
        String dirName = "folder" + rnd.nextInt(100);
        Path tmpDir = Files.createTempDirectory(dirName);

        String fileName = "afile" + rnd.nextInt(100);
        Path file = tmpDir.resolve(fileName);
        FileOutputStream fout = new FileOutputStream(file.toFile());
        byte[] fileContents = "IPFS rocks!".getBytes();
        fout.write(fileContents);
        fout.flush();
        fout.close();

        String subdirName = "subdir";
        tmpDir.resolve(subdirName).toFile().mkdir();

        String subfileName = "subdirfile" + rnd.nextInt(100);
        Path subdirfile = tmpDir.resolve(subdirName + "/" + subfileName);
        FileOutputStream fout2 = new FileOutputStream(subdirfile.toFile());
        byte[] file2Contents = "IPFS still rocks!".getBytes();
        fout2.write(file2Contents);
        fout2.flush();
        fout2.close();

        MerkleNode addResult = ipfs.add(new NamedStreamable.FileWrapper(tmpDir.toFile()));
        List<MerkleNode> lsResult = ipfs.ls(addResult.hash);
        if (lsResult.size() != 1)
            throw new IllegalStateException("Incorrect number of objects in ls!");
        if (!lsResult.get(0).equals(addResult))
            throw new IllegalStateException("Object not returned in ls!");
        byte[] catResult = ipfs.cat(addResult.hash, "/" + fileName);
        if (!Arrays.equals(catResult, fileContents))
            throw new IllegalStateException("Different contents!");

        byte[] catResult2 = ipfs.cat(addResult.hash, "/" + subdirName + "/" + subfileName);
        if (!Arrays.equals(catResult2, file2Contents))
            throw new IllegalStateException("Different contents!");
    }

//    @org.junit.Test
    public void largeFileTest() {
        byte[] largerData = new byte[100*1024*1024];
        new Random(1).nextBytes(largerData);
        NamedStreamable.ByteArrayWrapper largeFile = new NamedStreamable.ByteArrayWrapper("nontrivial.txt", largerData);
        fileTest(largeFile);
    }

//    @org.junit.Test
    public void hugeFileStreamTest() {
        byte[] hugeData = new byte[1000*1024*1024];
        new Random(1).nextBytes(hugeData);
        NamedStreamable.ByteArrayWrapper largeFile = new NamedStreamable.ByteArrayWrapper("massive.txt", hugeData);
        try {
            MerkleNode addResult = ipfs.add(largeFile);
            InputStream in = ipfs.catStream(addResult.hash);

            byte[] res = new byte[hugeData.length];
            int offset = 0;
            byte[] buf = new byte[4096];
            int r;
            while ((r = in.read(buf)) >= 0) {
                try {
                    System.arraycopy(buf, 0, res, offset, r);
                    offset += r;
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            if (!Arrays.equals(res, hugeData))
                throw new IllegalStateException("Different contents!");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @org.junit.Test
    public void hostFileTest() throws IOException {
        Path tempFile = Files.createTempFile("IPFS", "tmp");
        BufferedWriter w = new BufferedWriter(new FileWriter(tempFile.toFile()));
        w.append("Some data");
        w.flush();
        w.close();
        NamedStreamable hostFile = new NamedStreamable.FileWrapper(tempFile.toFile());
        fileTest(hostFile);
    }

    public void fileTest(NamedStreamable file) {
        try {
            MerkleNode addResult = ipfs.add(file);
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
            Map<Multihash, Object> ls = ipfs.pin.ls(IPFS.PinType.recursive);
            ipfs.repo.gc();
            // object should still be present after gc
            Map<Multihash, Object> ls2 = ipfs.pin.ls(IPFS.PinType.recursive);
            boolean stillPinned = ls2.containsKey(hash);
            Assert.assertTrue("Pinning works", pinned && stillPinned);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @org.junit.Test
    public void pinUpdate() {
        try {
            MerkleNode child1 = ipfs.add(new NamedStreamable.ByteArrayWrapper("some data".getBytes()));
            Multihash hashChild1 = child1.hash;
            System.out.println("child1: " + hashChild1);

            CborObject.CborMerkleLink root1 = new CborObject.CborMerkleLink(hashChild1);
            MerkleNode root1Res = ipfs.block.put(Collections.singletonList(root1.toByteArray()), Optional.of("cbor")).get(0);
            System.out.println("root1: " + root1Res.hash);
            ipfs.pin.add(root1Res.hash);

            CborObject.CborList root2 = new CborObject.CborList(Arrays.asList(new CborObject.CborMerkleLink(hashChild1), new CborObject.CborLong(42)));
            MerkleNode root2Res = ipfs.block.put(Collections.singletonList(root2.toByteArray()), Optional.of("cbor")).get(0);
            List<MultiAddress> update = ipfs.pin.update(root1Res.hash, root2Res.hash, true);

            Map<Multihash, Object> ls = ipfs.pin.ls(IPFS.PinType.all);
            boolean childPresent = ls.containsKey(hashChild1);
            if (!childPresent)
                throw new IllegalStateException("Child not present!");

            ipfs.repo.gc();
            Map<Multihash, Object> ls2 = ipfs.pin.ls(IPFS.PinType.all);
            boolean childPresentAfterGC = ls2.containsKey(hashChild1);
            if (!childPresentAfterGC)
                throw new IllegalStateException("Child not present!");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @org.junit.Test
    public void rawLeafNodePinUpdate() {
        try {
            MerkleNode child1 = ipfs.block.put("some data".getBytes(), Optional.of("raw"));
            Multihash hashChild1 = child1.hash;
            System.out.println("child1: " + hashChild1.type);

            CborObject.CborMerkleLink root1 = new CborObject.CborMerkleLink(hashChild1);
            MerkleNode root1Res = ipfs.block.put(Collections.singletonList(root1.toByteArray()), Optional.of("cbor")).get(0);
            System.out.println("root1: " + root1Res.hash);
            ipfs.pin.add(root1Res.hash);

            MerkleNode child2 = ipfs.block.put("G'day new tree".getBytes(), Optional.of("raw"));
            Multihash hashChild2 = child2.hash;

            CborObject.CborList root2 = new CborObject.CborList(Arrays.asList(
                    new CborObject.CborMerkleLink(hashChild1),
                    new CborObject.CborMerkleLink(hashChild2),
                    new CborObject.CborLong(42))
            );
            MerkleNode root2Res = ipfs.block.put(Collections.singletonList(root2.toByteArray()), Optional.of("cbor")).get(0);
            List<MultiAddress> update = ipfs.pin.update(root1Res.hash, root2Res.hash, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @org.junit.Test
    public void indirectPinTest() {
        try {
            Multihash EMPTY = ipfs.object._new(Optional.empty()).hash;
            io.ipfs.api.MerkleNode data = ipfs.object.patch(EMPTY, "set-data", Optional.of("childdata".getBytes()), Optional.empty(), Optional.empty());
            Multihash child = data.hash;

            io.ipfs.api.MerkleNode tmp1 = ipfs.object.patch(EMPTY, "set-data", Optional.of("parent1_data".getBytes()), Optional.empty(), Optional.empty());
            Multihash parent1 = ipfs.object.patch(tmp1.hash, "add-link", Optional.empty(), Optional.of(child.toString()), Optional.of(child)).hash;
            ipfs.pin.add(parent1);

            io.ipfs.api.MerkleNode tmp2 = ipfs.object.patch(EMPTY, "set-data", Optional.of("parent2_data".getBytes()), Optional.empty(), Optional.empty());
            Multihash parent2 = ipfs.object.patch(tmp2.hash, "add-link", Optional.empty(), Optional.of(child.toString()), Optional.of(child)).hash;
            ipfs.pin.add(parent2);
            ipfs.pin.rm(parent1, true);

            Map<Multihash, Object> ls = ipfs.pin.ls(IPFS.PinType.all);
            boolean childPresent = ls.containsKey(child);
            if (!childPresent)
                throw new IllegalStateException("Child not present!");

            ipfs.repo.gc();
            Map<Multihash, Object> ls2 = ipfs.pin.ls(IPFS.PinType.all);
            boolean childPresentAfterGC = ls2.containsKey(child);
            if (!childPresentAfterGC)
                throw new IllegalStateException("Child not present!");
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
//            byte[] data = "some random textual data".getBytes();
            byte[] data = new byte[1024];
            new Random().nextBytes(data);
            MerkleNode patched = ipfs.object.patch(base, "set-data", Optional.of(data), Optional.empty(), Optional.empty());
            byte[] patchedResult = ipfs.object.data(patched.hash);
            if (!Arrays.equals(patchedResult, data))
                throw new RuntimeException("object.patch: returned data != stored data!");

            MerkleNode twicePatched = ipfs.object.patch(patched.hash, "append-data", Optional.of(data), Optional.empty(), Optional.empty());
            byte[] twicePatchedResult = ipfs.object.data(twicePatched.hash);
            byte[] twice = new byte[2*data.length];
            for (int i=0; i < 2; i++)
                System.arraycopy(data, 0, twice, i*data.length, data.length);
            if (!Arrays.equals(twicePatchedResult, twice))
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
    public void bulkBlockTest() {
        try {
            CborObject cbor = new CborObject.CborString("G'day IPFS!");
            byte[] raw = cbor.toByteArray();
            List<MerkleNode> bulkPut = ipfs.block.put(Arrays.asList(raw, raw, raw, raw, raw), Optional.of("cbor"));
            List<Multihash> hashes = bulkPut.stream().map(m -> m.hash).collect(Collectors.toList());
            byte[] result = ipfs.block.get(hashes.get(0));
            System.out.println();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String toEscapedHex(byte[] in) throws IOException {
        StringBuilder res = new StringBuilder();
        for (byte b : in) {
            res.append("\\x");
            res.append(String.format("%02x", b & 0xFF));
        }
        return res.toString();
    }

    /**
     *  Test that merkle links in values of a cbor map are followed during recursive pins
     */
    @org.junit.Test
    public void merkleLinkInMap() {
        try {
            Random r = new Random();
            CborObject.CborByteArray target = new CborObject.CborByteArray(("g'day IPFS!").getBytes());
            byte[] rawTarget = target.toByteArray();
            MerkleNode targetRes = ipfs.block.put(Arrays.asList(rawTarget), Optional.of("cbor")).get(0);

            CborObject.CborMerkleLink link = new CborObject.CborMerkleLink(targetRes.hash);
            Map<String, CborObject> m = new TreeMap<>();
            m.put("alink", link);
            m.put("arr", new CborObject.CborList(Collections.emptyList()));
            CborObject.CborMap source = CborObject.CborMap.build(m);
            byte[] rawSource = source.toByteArray();
            MerkleNode sourceRes = ipfs.block.put(Arrays.asList(rawSource), Optional.of("cbor")).get(0);

            CborObject.fromByteArray(rawSource);

            List<Multihash> add = ipfs.pin.add(sourceRes.hash);
            ipfs.repo.gc();
            ipfs.repo.gc();

            List<Multihash> refs = ipfs.refs(sourceRes.hash, true);
            Assert.assertTrue("refs returns links", refs.contains(targetRes.hash));

            byte[] bytes = ipfs.block.get(targetRes.hash);
            Assert.assertTrue("same contents after GC", Arrays.equals(bytes, rawTarget));
            // These commands can be used to reproduce this on the command line
            String reproCommand1 = "printf \"" + toEscapedHex(rawTarget) + "\" | ipfs block put --format=cbor";
            String reproCommand2 = "printf \"" + toEscapedHex(rawSource) + "\" | ipfs block put --format=cbor";
            System.out.println();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @org.junit.Test
    public void recursiveRefs() {
        try {
            CborObject.CborByteArray leaf1 = new CborObject.CborByteArray(("G'day IPFS!").getBytes());
            byte[] rawLeaf1 = leaf1.toByteArray();
            MerkleNode leaf1Res = ipfs.block.put(Arrays.asList(rawLeaf1), Optional.of("cbor")).get(0);

            CborObject.CborMerkleLink link = new CborObject.CborMerkleLink(leaf1Res.hash);
            Map<String, CborObject> m = new TreeMap<>();
            m.put("link1", link);
            CborObject.CborMap source = CborObject.CborMap.build(m);
            MerkleNode sourceRes = ipfs.block.put(Arrays.asList(source.toByteArray()), Optional.of("cbor")).get(0);

            CborObject.CborByteArray leaf2 = new CborObject.CborByteArray(("G'day again, IPFS!").getBytes());
            byte[] rawLeaf2 = leaf2.toByteArray();
            MerkleNode leaf2Res = ipfs.block.put(Arrays.asList(rawLeaf2), Optional.of("cbor")).get(0);

            Map<String, CborObject> m2 = new TreeMap<>();
            m2.put("link1", new CborObject.CborMerkleLink(sourceRes.hash));
            m2.put("link2", new CborObject.CborMerkleLink(leaf2Res.hash));
            CborObject.CborMap source2 = CborObject.CborMap.build(m2);
            MerkleNode rootRes = ipfs.block.put(Arrays.asList(source2.toByteArray()), Optional.of("cbor")).get(0);

            List<Multihash> refs = ipfs.refs(rootRes.hash, false);
            boolean correct = refs.contains(sourceRes.hash) && refs.contains(leaf2Res.hash) && refs.size() == 2;
            Assert.assertTrue("refs returns links", correct);

            List<Multihash> refsRecurse = ipfs.refs(rootRes.hash, true);
            boolean correctRecurse = refs.contains(sourceRes.hash)
                    && refs.contains(leaf1Res.hash)
                    && refs.contains(leaf2Res.hash)
                    && refs.size() == 3;
            Assert.assertTrue("refs returns links", correct);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *  Test that merkle links as a root object are followed during recursive pins
     */
    @org.junit.Test
    public void rootMerkleLink() {
        try {
            Random r = new Random();
            CborObject.CborByteArray target = new CborObject.CborByteArray(("g'day IPFS!" + r.nextInt()).getBytes());
            byte[] rawTarget = target.toByteArray();
            MerkleNode block1 = ipfs.block.put(Arrays.asList(rawTarget), Optional.of("cbor")).get(0);
            Multihash block1Hash = block1.hash;
            byte[] retrievedObj1 = ipfs.block.get(block1Hash);
            Assert.assertTrue("get inverse of put", Arrays.equals(retrievedObj1, rawTarget));

            CborObject.CborMerkleLink cbor2 = new CborObject.CborMerkleLink(block1.hash);
            byte[] obj2 = cbor2.toByteArray();
            MerkleNode block2 = ipfs.block.put(Arrays.asList(obj2), Optional.of("cbor")).get(0);
            byte[] retrievedObj2 = ipfs.block.get(block2.hash);
            Assert.assertTrue("get inverse of put", Arrays.equals(retrievedObj2, obj2));

            List<Multihash> add = ipfs.pin.add(block2.hash);
            ipfs.repo.gc();
            ipfs.repo.gc();

            byte[] bytes = ipfs.block.get(block1.hash);
            Assert.assertTrue("same contents after GC", Arrays.equals(bytes, rawTarget));
            // These commands can be used to reproduce this on the command line
            String reproCommand1 = "printf \"" + toEscapedHex(rawTarget) + "\" | ipfs block put --format=cbor";
            String reproCommand2 = "printf \"" + toEscapedHex(obj2) + "\" | ipfs block put --format=cbor";
            System.out.println();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *  Test that merkle links as a root object are followed during recursive pins
     */
    @org.junit.Test
    public void rootNull() {
        try {
            CborObject.CborNull cbor = new CborObject.CborNull();
            byte[] obj = cbor.toByteArray();
            MerkleNode block = ipfs.block.put(Arrays.asList(obj), Optional.of("cbor")).get(0);
            byte[] retrievedObj = ipfs.block.get(block.hash);
            Assert.assertTrue("get inverse of put", Arrays.equals(retrievedObj, obj));

            List<Multihash> add = ipfs.pin.add(block.hash);
            ipfs.repo.gc();
            ipfs.repo.gc();

            // These commands can be used to reproduce this on the command line
            String reproCommand1 = "printf \"" + toEscapedHex(obj) + "\" | ipfs block put --format=cbor";
            System.out.println();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *  Test that merkle links in a cbor list are followed during recursive pins
     */
    @org.junit.Test
    public void merkleLinkInList() {
        try {
            Random r = new Random();
            CborObject.CborByteArray target = new CborObject.CborByteArray(("g'day IPFS!" + r.nextInt()).getBytes());
            byte[] rawTarget = target.toByteArray();
            MerkleNode targetRes = ipfs.block.put(Arrays.asList(rawTarget), Optional.of("cbor")).get(0);

            CborObject.CborMerkleLink link = new CborObject.CborMerkleLink(targetRes.hash);
            CborObject.CborList source = new CborObject.CborList(Arrays.asList(link));
            byte[] rawSource = source.toByteArray();
            MerkleNode sourceRes = ipfs.block.put(Arrays.asList(rawSource), Optional.of("cbor")).get(0);

            List<Multihash> add = ipfs.pin.add(sourceRes.hash);
            ipfs.repo.gc();
            ipfs.repo.gc();

            byte[] bytes = ipfs.block.get(targetRes.hash);
            Assert.assertTrue("same contents after GC", Arrays.equals(bytes, rawTarget));
            // These commands can be used to reproduce this on the command line
            String reproCommand1 = "printf \"" + toEscapedHex(rawTarget) + "\" | ipfs block put --format=cbor";
            String reproCommand2 = "printf \"" + toEscapedHex(rawSource) + "\" | ipfs block put --format=cbor";
            System.out.println();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @org.junit.Test
    public void fileContentsTest() {
        try {
            ipfs.repo.gc();
            List<Multihash> local = ipfs.refs.local();
            for (Multihash hash: local) {
                try {
                    Map ls = ipfs.file.ls(hash);
                    return;
                } catch (Exception e) {} // non unixfs files will throw an exception here
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
            String name = "key" + System.nanoTime();
            Object gen = ipfs.key.gen(name, Optional.of("rsa"), Optional.of("2048"));
            Map mykey = ipfs.name.publish(pointer.hash, Optional.of(name));
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
            List<Peer> peers = ipfs.swarm.peers();
            Map query = ipfs.dht.query(peers.get(0).address);
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
            String multiaddr = "/ip4/127.0.0.1/tcp/4001/ipfs/QmaCpDMGvV2BGHeYERUEnRQAwe3N8SzbUtfsmvsqQLuvuJ";
            Map connect = ipfs.swarm.connect(multiaddr);
            Map disconnect = ipfs.swarm.disconnect(multiaddr);
            Map<String, Object> addrs = ipfs.swarm.addrs();
            if (addrs.size() > 0) {
                boolean contacted = addrs.keySet().stream()
                        .filter(target -> {
                            try {
                                Map id = ipfs.id(target);
                                Map ping = ipfs.ping(target);
                                return true;
                            } catch (Exception e) {
                                // not all nodes have to be contactable
                                return false;
                            }
                        }).findAny().isPresent();
                if (!contacted)
                    throw new IllegalStateException("Couldn't contact any node!");
            }
            List<Peer> peers = ipfs.swarm.peers();
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
            String sys = ipfs.diag.sys();
            String cmds = ipfs.diag.cmds();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @org.junit.Test
    public void toolsTest() {
        try {
            String version = ipfs.version();
            int major = Integer.parseInt(version.split("\\.")[0]);
            int minor = Integer.parseInt(version.split("\\.")[1]);
            assertTrue(major >= 0 && minor >= 4);     // Requires at least 0.4.0
            Map commands = ipfs.commands();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // this api is disabled until deployment over IPFS is enabled
    public void updateTest() {
        try {
            Object check = ipfs.update.check();
            Object update = ipfs.update();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
