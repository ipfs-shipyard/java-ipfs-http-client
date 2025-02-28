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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

@SuppressWarnings({"rawtypes", "unused"})
public class APITest {

    private final MultiAddress ipfsAddress = new MultiAddress("/ip4/127.0.0.1/tcp/5001");
    private final IPFS ipfs = new IPFS(ipfsAddress.getHost(), ipfsAddress.getPort(), "/api/v0/", true, false);

    private final Random r = new Random(33550336); // perfect

    @Test
    public void dag() throws IOException {
        String original = "{\"data\":1234}";
        byte[] object = original.getBytes();
        MerkleNode put = ipfs.dag.put("json", object);

        Cid expected = Cid.decode("bafyreidbm2zncsc3j25zn7lofgd4woeh6eygdy73thfosuni2rwr3bhcvu");

        Multihash result = put.hash;
        assertEquals("Correct cid returned", result, expected);

        byte[] get = ipfs.dag.get(expected);
        assertEquals("Raw data equal", original, new String(get).trim());
        Map res = ipfs.dag.resolve("bafyreidbm2zncsc3j25zn7lofgd4woeh6eygdy73thfosuni2rwr3bhcvu");
        assertNotNull("not resolved", res);
        res = ipfs.dag.stat(expected);
        assertNotNull("not found", res);
    }

    @Test
    public void dagCbor() throws IOException {
        Map<String, CborObject> tmp = new LinkedHashMap<>();
        String value = "G'day mate!";
        tmp.put("data", new CborObject.CborString(value));
        CborObject original = CborObject.CborMap.build(tmp);
        byte[] object = original.toByteArray();
        MerkleNode put = ipfs.dag.put("dag-cbor", object);

        Cid cid = (Cid) put.hash;

        byte[] get = ipfs.dag.get(cid);
        assertEquals("Raw data equal", ((Map) JSONParser.parse(new String(get))).get("data"),
            value);

        Cid expected = Cid.decode("zdpuApemz4XMURSCkBr9W5y974MXkSbeDfLeZmiQTPpvkatFF");
        assertEquals("Correct cid returned", cid, expected);
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
        assertEquals("removed key", remaining, existing);
    }

    @Test
    @Ignore("Not reliable")
    public void log() throws IOException {
        Map lsResult = ipfs.log.ls();
        Assert.assertFalse("Log ls", lsResult.isEmpty());
        Map levelResult = ipfs.log.level("all", "info");
        Assert.assertTrue("Log level", ((String)levelResult.get("Message")).startsWith("Changed log level"));
    }

    @Test
    public void ipldNode() {
        Function<Stream<Pair<String, CborObject>>, CborObject.CborMap> map =
                s -> CborObject.CborMap.build(s.collect(Collectors.toMap(p -> p.left, p -> p.right)));
        CborObject.CborMap a = map.apply(Stream.of(new Pair<>("b", new CborObject.CborLong(1))));

        CborObject.CborMap cbor = map.apply(Stream.of(new Pair<>("a", a), new Pair<>("c", new CborObject.CborLong(2))));

        IpldNode.CborIpldNode node = new IpldNode.CborIpldNode(cbor);
        List<String> tree = node.tree("", -1);
        assertEquals("Correct tree", tree, Arrays.asList("/a/b", "/c"));
    }

    @Test
    public void singleFileTest() throws IOException {
        NamedStreamable.ByteArrayWrapper file = new NamedStreamable.ByteArrayWrapper("hello.txt", "G'day world! IPFS rocks!".getBytes());
        fileTest(file);
    }

    @Test
    public void wrappedSingleFileTest() throws IOException {
        NamedStreamable.ByteArrayWrapper file = new NamedStreamable.ByteArrayWrapper("hello.txt", "G'day world! IPFS rocks!".getBytes());
        List<MerkleNode> addParts = ipfs.add(file, true);
        MerkleNode filePart = addParts.get(0);
        MerkleNode dirPart = addParts.get(1);
        byte[] catResult = ipfs.cat(filePart.hash);
        byte[] getResult = ipfs.get(filePart.hash);
        if (!Arrays.equals(catResult, file.getContents()))
            throw new IllegalStateException("Different contents!");
        List<Multihash> pinRm = ipfs.pin.rm(dirPart.hash, true);
        if (!pinRm.get(0).equals(dirPart.hash))
            throw new IllegalStateException("Didn't remove file!");
        Object gc = ipfs.repo.gc();
    }

    @Test
    public void dirTest() throws IOException {
        Path test = Files.createTempDirectory("test");
        Files.write(test.resolve("file.txt"), "G'day IPFS!".getBytes());
        NamedStreamable dir = new NamedStreamable.FileWrapper(test.toFile());
        List<MerkleNode> add = ipfs.add(dir);
        MerkleNode addResult = add.get(add.size() - 1);
        List<MerkleNode> ls = ipfs.ls(addResult.hash);
        Assert.assertTrue(ls.size() > 0);
    }

    @Test
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

        List<MerkleNode> addParts = ipfs.add(new NamedStreamable.FileWrapper(tmpDir.toFile()));
        MerkleNode addResult = addParts.get(addParts.size() - 1);
        List<MerkleNode> lsResult = ipfs.ls(addResult.hash);
        if (lsResult.size() != 2)
            throw new IllegalStateException("Incorrect number of objects in ls!");
        if (! lsResult.stream().map(x -> x.name.get()).collect(Collectors.toSet()).equals(new HashSet<>(Arrays.asList(subdirName, fileName))))
            throw new IllegalStateException("Dir not returned in ls!");
        byte[] catResult = ipfs.cat(addResult.hash, "/" + fileName);
        if (! Arrays.equals(catResult, fileContents))
            throw new IllegalStateException("Different contents!");

        byte[] catResult2 = ipfs.cat(addResult.hash, "/" + subdirName + "/" + subfileName);
        if (! Arrays.equals(catResult2, file2Contents))
            throw new IllegalStateException("Different contents!");
    }

    @Ignore
    @Test
    public void largeFileTest() throws IOException {
        byte[] largerData = new byte[100*1024*1024];
        new Random(1).nextBytes(largerData);
        NamedStreamable.ByteArrayWrapper largeFile = new NamedStreamable.ByteArrayWrapper("nontrivial.txt", largerData);
        fileTest(largeFile);
    }

    @Ignore
    @Test
    public void hugeFileStreamTest() throws IOException {
        byte[] hugeData = new byte[1000*1024*1024];
        new Random(1).nextBytes(hugeData);
        NamedStreamable.ByteArrayWrapper largeFile = new NamedStreamable.ByteArrayWrapper("massive.txt", hugeData);
        MerkleNode addResult = ipfs.add(largeFile).get(0);
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
    }

    @Test
    public void hostFileTest() throws IOException {
        Path tempFile = Files.createTempFile("IPFS", "tmp");
        BufferedWriter w = new BufferedWriter(new FileWriter(tempFile.toFile()));
        w.append("Some data");
        w.flush();
        w.close();
        NamedStreamable hostFile = new NamedStreamable.FileWrapper(tempFile.toFile());
        fileTest(hostFile);
    }

    @Test
    public void hashOnly() throws IOException {
        byte[] data = randomBytes(4096);
        NamedStreamable file = new NamedStreamable.ByteArrayWrapper(data);
        MerkleNode addResult = ipfs.add(file, false, true).get(0);
        List<Multihash> local = ipfs.refs.local();
        if (local.contains(addResult.hash))
            throw new IllegalStateException("Object shouldn't be present!");
    }

    public void fileTest(NamedStreamable file)  throws IOException{
        MerkleNode addResult = ipfs.add(file).get(0);
        byte[] catResult = ipfs.cat(addResult.hash);
        byte[] getResult = ipfs.get(addResult.hash);
        if (!Arrays.equals(catResult, file.getContents()))
            throw new IllegalStateException("Different contents!");
        List<Multihash> pinRm = ipfs.pin.rm(addResult.hash, true);
        if (!pinRm.get(0).equals(addResult.hash))
            throw new IllegalStateException("Didn't remove file!");
        Object gc = ipfs.repo.gc();
    }
    @Test
    public void filesTest() throws IOException {

        ipfs.files.rm("/filesTest", true, true);
        String filename = "hello.txt";
        String folder = "/filesTest/one/two";
        String path = folder + "/" + filename;
        String contents = "hello world!";
        NamedStreamable ns = new NamedStreamable.ByteArrayWrapper(filename, contents.getBytes());
        String res = ipfs.files.write(path, ns, true, true);
        Map stat = ipfs.files.stat( path);
        Map stat2 = ipfs.files.stat( path, Optional.of("<cumulsize>"), true);
        String readContents = new String(ipfs.files.read(path));
        assertEquals("Should be equals", contents, readContents);
        res = ipfs.files.rm(path, false, false);

        String tempFilename = "temp.txt";
        String tempFolder = "/filesTest/a/b/c";
        String tempPath = tempFolder + "/" + tempFilename;
        String mkdir = ipfs.files.mkdir(tempFolder, true);
        stat = ipfs.files.stat(tempFolder);
        NamedStreamable tempFile = new NamedStreamable.ByteArrayWrapper(tempFilename, contents.getBytes());
        res = ipfs.files.write(tempPath, tempFile, true, false);
        res = ipfs.files.mv(tempPath, "/" + tempFilename);
        stat = ipfs.files.stat("/" + tempFilename);
        List<Map> lsMap = ipfs.files.ls("/");
        List<Map> lsMap2 = ipfs.files.ls("/", true, false);

        String flushFolder = "/filesTest/f/l/u/s/h";
        res = ipfs.files.mkdir(flushFolder, true);
        Map flushMap = ipfs.files.flush(flushFolder);

        String copyFilename = "copy.txt";
        String copyFromFolder = "/filesTest/fromThere";
        String copyToFolder = "/filesTest/toHere";
        String copyFromPath = copyFromFolder + "/" + copyFilename;
        String copyToPath = copyToFolder + "/" + copyFilename;
        NamedStreamable copyFile = new NamedStreamable.ByteArrayWrapper(copyFilename, "copy".getBytes());
        WriteFilesArgs args = WriteFilesArgs.Builder.newInstance()
                .setCreate()
                .setParents()
                .build();
        res = ipfs.files.write(copyFromPath, copyFile, args);
        res = ipfs.files.cp(copyFromPath, copyToPath, true);
        stat = ipfs.files.stat(copyToPath);
        String cidRes = ipfs.files.chcid(copyToPath);
        stat = ipfs.files.stat(copyToPath);
        String cidV0Res = ipfs.files.chcid(copyToPath, Optional.of(0), Optional.empty());
        stat = ipfs.files.stat(copyToPath);
        ipfs.files.rm("/filesTest", false, true);
    }

    @Test
    public void multibaseTest() throws IOException {
        List<Map> encodings = ipfs.multibase.list(true, false);
        Assert.assertFalse("multibase/list works", encodings.isEmpty());
        String encoded = ipfs.multibase.encode(Optional.empty(), new NamedStreamable.ByteArrayWrapper("hello".getBytes()));
        assertEquals("multibase/encode works", "uaGVsbG8", encoded);
        String decoded = ipfs.multibase.decode(new NamedStreamable.ByteArrayWrapper(encoded.getBytes()));
        assertEquals("multibase/decode works", "hello", decoded);
        String input = "f68656c6c6f";
        String transcode = ipfs.multibase.transcode(Optional.of("base64url"), new NamedStreamable.ByteArrayWrapper(input.getBytes()));
        assertEquals("multibase/transcode works", transcode, encoded);
    }

    @Test
    @Ignore("Experimental feature not enabled by default")
    public void fileStoreTest() throws IOException {
        ipfs.fileStore.dups();
        Map res = ipfs.fileStore.ls(true);
        ipfs.fileStore.verify(true);
    }

    @Test
    public void pinTest() throws IOException {
        MerkleNode file = ipfs.add(new NamedStreamable.ByteArrayWrapper("some data".getBytes())).get(0);
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
    }

    @Test
    @Ignore
    public void remotePinTest() throws IOException {
        MerkleNode file = ipfs.add(new NamedStreamable.ByteArrayWrapper("test data".getBytes())).get(0);
        Multihash hash = file.hash;
        String service = "mock";
        String rmRemoteService = ipfs.pin.remote.rmService(service);
        List<Map> lsRemoteService = ipfs.pin.remote.lsService(false);
        String endpoint = "http://127.0.0.1:3000";
        String key = "SET_VALUE_HERE";
        String added = ipfs.pin.remote.addService(service, endpoint, key);
        lsRemoteService = ipfs.pin.remote.lsService(false);
        Map addHash = ipfs.pin.remote.add(service, hash, Optional.empty(), true);
        Map lsRemote = ipfs.pin.remote.ls(service, Optional.empty(), Optional.of(List.of(IPFS.PinStatus.values())));
        String rmRemote = ipfs.pin.remote.rm(service, Optional.empty(), Optional.of(List.of(IPFS.PinStatus.queued)), Optional.of(List.of(hash)));
        lsRemote = ipfs.pin.remote.ls(service, Optional.empty(), Optional.of(List.of(IPFS.PinStatus.values())));
    }

    @Test
    public void pinUpdate() throws IOException {
        MerkleNode child1 = ipfs.add(new NamedStreamable.ByteArrayWrapper("some data".getBytes())).get(0);
        Multihash hashChild1 = child1.hash;

        CborObject.CborMerkleLink root1 = new CborObject.CborMerkleLink(hashChild1);
        MerkleNode root1Res = ipfs.block.put(Collections.singletonList(root1.toByteArray()), Optional.of("cbor")).get(0);
        ipfs.pin.add(root1Res.hash);

        CborObject.CborList root2 = new CborObject.CborList(Arrays.asList(new CborObject.CborMerkleLink(hashChild1), new CborObject.CborLong(System.currentTimeMillis())));
        MerkleNode root2Res = ipfs.block.put(Collections.singletonList(root2.toByteArray()), Optional.of("cbor")).get(0);
        List<Multihash> update = ipfs.pin.update(root1Res.hash, root2Res.hash, true);

        Map<Multihash, Object> ls = ipfs.pin.ls(IPFS.PinType.all);
        boolean childPresent = ls.containsKey(hashChild1);
        if (!childPresent)
            throw new IllegalStateException("Child not present!");

        ipfs.repo.gc();
        Map<Multihash, Object> ls2 = ipfs.pin.ls(IPFS.PinType.all);
        boolean childPresentAfterGC = ls2.containsKey(hashChild1);
        if (!childPresentAfterGC)
            throw new IllegalStateException("Child not present!");
    }

    @Test
    public void rawLeafNodePinUpdate() throws IOException {
        MerkleNode child1 = ipfs.block.put("some data".getBytes(), Optional.of("raw"));
        Multihash hashChild1 = child1.hash;

        CborObject.CborMerkleLink root1 = new CborObject.CborMerkleLink(hashChild1);
        MerkleNode root1Res = ipfs.block.put(Collections.singletonList(root1.toByteArray()), Optional.of("cbor")).get(0);
        ipfs.pin.add(root1Res.hash);

        MerkleNode child2 = ipfs.block.put("G'day new tree".getBytes(), Optional.of("raw"));
        Multihash hashChild2 = child2.hash;

        CborObject.CborList root2 = new CborObject.CborList(Arrays.asList(
                new CborObject.CborMerkleLink(hashChild1),
                new CborObject.CborMerkleLink(hashChild2),
                new CborObject.CborLong(System.currentTimeMillis()))
        );
        MerkleNode root2Res = ipfs.block.put(Collections.singletonList(root2.toByteArray()), Optional.of("cbor")).get(0);
        List<Multihash> update = ipfs.pin.update(root1Res.hash, root2Res.hash, false);
    }

    @Test
    public void indirectPinTest() throws IOException {
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
}

    @Test
    public void objectPatch() throws IOException {
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

    }

    @Test
    public void refsTest() throws IOException {
        List<Multihash> local = ipfs.refs.local();
        for (Multihash ref: local) {
            Object refs = ipfs.refs(ref, false);
        }
    }

    @Test
    public void objectTest() throws IOException {
        MerkleNode _new = ipfs.object._new(Optional.empty());
        Multihash pointer = Multihash.fromBase58("QmPZ9gcCEpqKTo6aq61g2nXGUhM4iCL3ewB6LDXZCtioEB");
        MerkleNode object = ipfs.object.get(pointer);
        List<MerkleNode> newPointer = ipfs.object.put(Collections.singletonList(object.toJSONString().getBytes()));
        List<MerkleNode> newPointer2 = ipfs.object.put("json", Collections.singletonList(object.toJSONString().getBytes()));
        MerkleNode links = ipfs.object.links(pointer);
        byte[] data = ipfs.object.data(pointer);
        Map stat = ipfs.object.stat(pointer);
    }

    @Test
    public void blockTest() throws IOException {
        MerkleNode pointer = new MerkleNode("QmPZ9gcCEpqKTo6aq61g2nXGUhM4iCL3ewB6LDXZCtioEB");
        Map stat = ipfs.block.stat(pointer.hash);
        byte[] object = ipfs.block.get(pointer.hash);
        List<MerkleNode> newPointer = ipfs.block.put(Collections.singletonList("Some random data...".getBytes()));
    }

    @Test
    public void bulkBlockTest() throws IOException {
        CborObject cbor = new CborObject.CborString("G'day IPFS!");
        byte[] raw = cbor.toByteArray();
        List<MerkleNode> bulkPut = ipfs.block.put(Arrays.asList(raw, raw, raw, raw, raw), Optional.of("cbor"));
        List<Multihash> hashes = bulkPut.stream().map(m -> m.hash).collect(Collectors.toList());
        byte[] result = ipfs.block.get(hashes.get(0));
        System.out.println();
    }

//    @Ignore // Ignored because ipfs frequently times out internally in the publish call
    @Test
    public void publish() throws Exception {
        // JSON document
        String json = "{\"name\":\"blogpost\",\"documents\":[]}";

        // Add a DAG node to IPFS
        MerkleNode merkleNode = ipfs.dag.put("json", json.getBytes());
        assertEquals("expected to be bafyreiafmbgul64c4nyybvgivswmkuhifamc24cdfuj4ij5xtnhpsfelky" , "bafyreiafmbgul64c4nyybvgivswmkuhifamc24cdfuj4ij5xtnhpsfelky", merkleNode.hash.toString());

        // Get a DAG node
        byte[] res = ipfs.dag.get((Cid) merkleNode.hash);
        assertEquals("Should be equals", JSONParser.parse(json), JSONParser.parse(new String(res)));

        // Publish to IPNS
        Map result = ipfs.name.publish(merkleNode.hash);

        // Resolve from IPNS
        String resolved = ipfs.name.resolve(Cid.decode((String) result.get("Name")));
        assertEquals("Should be equals", resolved, "/ipfs/" + merkleNode.hash);
    }

    @Test
    public void pubsubSynchronous() {
        String topic = "topic" + System.nanoTime();
        List<Map<String, Object>> res = Collections.synchronizedList(new ArrayList<>());
        new Thread(() -> {
            try {
                ipfs.pubsub.sub(topic, res::add, t -> t.printStackTrace());
            } catch (IOException e) {
                throw new RuntimeException(e);}
        }).start();

        int nMessages = 100;
        for (int i = 1; i < nMessages; ) {
            ipfs.pubsub.pub(topic, "Hello World!");
            if (res.size() >= i) {
                i++;
            }
        }
        Assert.assertTrue(res.size() > nMessages - 5); // pubsub is not reliable so it loses messages
    }

    @Test
    public void pubsub() throws Exception {
        String topic = "topic" + System.nanoTime();
        Stream<Map<String, Object>> sub = ipfs.pubsub.sub(topic);
        String data = "Hello World!";
        ipfs.pubsub.pub(topic, data);
        ipfs.pubsub.pub(topic, "G'day");
        List<Map> results = sub.limit(2).collect(Collectors.toList());
        Assert.assertNotEquals(results.get(0), Collections.emptyMap());
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
    @Test
    public void merkleLinkInMap() throws IOException {
        Random r = new Random();
        CborObject.CborByteArray target = new CborObject.CborByteArray(("g'day IPFS!").getBytes());
        byte[] rawTarget = target.toByteArray();
        MerkleNode targetRes = ipfs.block.put(Collections.singletonList(rawTarget), Optional.of("cbor")).get(0);

        CborObject.CborMerkleLink link = new CborObject.CborMerkleLink(targetRes.hash);
        Map<String, CborObject> m = new TreeMap<>();
        m.put("alink", link);
        m.put("arr", new CborObject.CborList(Collections.emptyList()));
        CborObject.CborMap source = CborObject.CborMap.build(m);
        byte[] rawSource = source.toByteArray();
        MerkleNode sourceRes = ipfs.block.put(Collections.singletonList(rawSource), Optional.of("cbor")).get(0);

        CborObject.fromByteArray(rawSource);

        List<Multihash> add = ipfs.pin.add(sourceRes.hash);
        ipfs.repo.gc();
        ipfs.repo.gc();

        List<Multihash> refs = ipfs.refs(sourceRes.hash, true);
        Assert.assertTrue("refs returns links", refs.contains(targetRes.hash));

        byte[] bytes = ipfs.block.get(targetRes.hash);
        assertArrayEquals("same contents after GC", bytes, rawTarget);
        // These commands can be used to reproduce this on the command line
        String reproCommand1 = "printf \"" + toEscapedHex(rawTarget) + "\" | ipfs block put --format=cbor";
        String reproCommand2 = "printf \"" + toEscapedHex(rawSource) + "\" | ipfs block put --format=cbor";
        System.out.println();
    }

    @Test
    public void recursiveRefs() throws IOException {
        CborObject.CborByteArray leaf1 = new CborObject.CborByteArray(("G'day IPFS!").getBytes());
        byte[] rawLeaf1 = leaf1.toByteArray();
        MerkleNode leaf1Res = ipfs.block.put(Collections.singletonList(rawLeaf1), Optional.of("cbor")).get(0);

        CborObject.CborMerkleLink link = new CborObject.CborMerkleLink(leaf1Res.hash);
        Map<String, CborObject> m = new TreeMap<>();
        m.put("link1", link);
        CborObject.CborMap source = CborObject.CborMap.build(m);
        MerkleNode sourceRes = ipfs.block.put(Collections.singletonList(source.toByteArray()), Optional.of("cbor")).get(0);

        CborObject.CborByteArray leaf2 = new CborObject.CborByteArray(("G'day again, IPFS!").getBytes());
        byte[] rawLeaf2 = leaf2.toByteArray();
        MerkleNode leaf2Res = ipfs.block.put(Collections.singletonList(rawLeaf2), Optional.of("cbor")).get(0);

        Map<String, CborObject> m2 = new TreeMap<>();
        m2.put("link1", new CborObject.CborMerkleLink(sourceRes.hash));
        m2.put("link2", new CborObject.CborMerkleLink(leaf2Res.hash));
        CborObject.CborMap source2 = CborObject.CborMap.build(m2);
        MerkleNode rootRes = ipfs.block.put(Collections.singletonList(source2.toByteArray()), Optional.of("cbor")).get(0);

        List<Multihash> refs = ipfs.refs(rootRes.hash, false);
        boolean correct = refs.contains(sourceRes.hash) && refs.contains(leaf2Res.hash) && refs.size() == 2;
        Assert.assertTrue("refs returns links", correct);

        List<Multihash> refsRecurse = ipfs.refs(rootRes.hash, true);
        boolean correctRecurse = refsRecurse.contains(sourceRes.hash)
                && refsRecurse.contains(leaf1Res.hash)
                && refsRecurse.contains(leaf2Res.hash)
                && refsRecurse.size() == 3;
        Assert.assertTrue("refs returns links", correctRecurse);
    }

    /**
     *  Test that merkle links as a root object are followed during recursive pins
     */
    @Test
    public void rootMerkleLink() throws IOException {
        Random r = new Random();
        CborObject.CborByteArray target = new CborObject.CborByteArray(("g'day IPFS!" + r.nextInt()).getBytes());
        byte[] rawTarget = target.toByteArray();
        MerkleNode block1 = ipfs.block.put(Collections.singletonList(rawTarget), Optional.of("cbor")).get(0);
        Multihash block1Hash = block1.hash;
        byte[] retrievedObj1 = ipfs.block.get(block1Hash);
        assertArrayEquals("get inverse of put", retrievedObj1, rawTarget);

        CborObject.CborMerkleLink cbor2 = new CborObject.CborMerkleLink(block1.hash);
        byte[] obj2 = cbor2.toByteArray();
        MerkleNode block2 = ipfs.block.put(Collections.singletonList(obj2), Optional.of("cbor")).get(0);
        byte[] retrievedObj2 = ipfs.block.get(block2.hash);
        assertArrayEquals("get inverse of put", retrievedObj2, obj2);

        List<Multihash> add = ipfs.pin.add(block2.hash);
        ipfs.repo.gc();
        ipfs.repo.gc();

        byte[] bytes = ipfs.block.get(block1.hash);
        assertArrayEquals("same contents after GC", bytes, rawTarget);
        // These commands can be used to reproduce this on the command line
        String reproCommand1 = "printf \"" + toEscapedHex(rawTarget) + "\" | ipfs block put --format=cbor";
        String reproCommand2 = "printf \"" + toEscapedHex(obj2) + "\" | ipfs block put --format=cbor";
        System.out.println();
    }

    /**
     *  Test that a cbor null is allowed as an object root
     */
    @Test
    public void rootNull() throws IOException {
        CborObject.CborNull cbor = new CborObject.CborNull();
        byte[] obj = cbor.toByteArray();
        MerkleNode block = ipfs.block.put(Collections.singletonList(obj), Optional.of("cbor")).get(0);
        byte[] retrievedObj = ipfs.block.get(block.hash);
        assertArrayEquals("get inverse of put", retrievedObj, obj);

        List<Multihash> add = ipfs.pin.add(block.hash);
        ipfs.repo.gc();
        ipfs.repo.gc();

        // These commands can be used to reproduce this on the command line
        String reproCommand1 = "printf \"" + toEscapedHex(obj) + "\" | ipfs block put --format=cbor";
        System.out.println();
    }

    /**
     *  Test that merkle links in a cbor list are followed during recursive pins
     */
    @Test
    public void merkleLinkInList() throws IOException {
        Random r = new Random();
        CborObject.CborByteArray target = new CborObject.CborByteArray(("g'day IPFS!" + r.nextInt()).getBytes());
        byte[] rawTarget = target.toByteArray();
        MerkleNode targetRes = ipfs.block.put(Collections.singletonList(rawTarget), Optional.of("cbor")).get(0);

        CborObject.CborMerkleLink link = new CborObject.CborMerkleLink(targetRes.hash);
        CborObject.CborList source = new CborObject.CborList(Collections.singletonList(link));
        byte[] rawSource = source.toByteArray();
        MerkleNode sourceRes = ipfs.block.put(Collections.singletonList(rawSource), Optional.of("cbor")).get(0);

        List<Multihash> add = ipfs.pin.add(sourceRes.hash);
        ipfs.repo.gc();
        ipfs.repo.gc();

        byte[] bytes = ipfs.block.get(targetRes.hash);
        assertArrayEquals("same contents after GC", bytes, rawTarget);
        // These commands can be used to reproduce this on the command line
        String reproCommand1 = "printf \"" + toEscapedHex(rawTarget) + "\" | ipfs block put --format=cbor";
        String reproCommand2 = "printf \"" + toEscapedHex(rawSource) + "\" | ipfs block put --format=cbor";
    }

    @Test
    public void fileContentsTest() throws IOException {
        ipfs.repo.gc();
        List<Multihash> local = ipfs.refs.local();
        for (Multihash hash: local) {
            try {
                Map ls = ipfs.file.ls(hash);
                return;
            } catch (Exception e) {} // non unixfs files will throw an exception here
        }
    }

    @Test
    @Ignore
    public void repoTest() throws IOException {
        ipfs.repo.gc();
        Multihash res = ipfs.repo.ls();
        //String migration = ipfs.repo.migrate(false);
        RepoStat stat = ipfs.repo.stat(false);
        RepoStat stat2 = ipfs.repo.stat(true);
        Map verify = ipfs.repo.verify();
        Map version = ipfs.repo.version();
    }
    @Test
    @Ignore("name test may hang forever")
    public void nameTest() throws IOException {
        MerkleNode pointer = new MerkleNode("QmPZ9gcCEpqKTo6aq61g2nXGUhM4iCL3ewB6LDXZCtioEB");
        Map pub = ipfs.name.publish(pointer.hash);
        String name = "key" + System.nanoTime();
        Object gen = ipfs.key.gen(name, Optional.of("rsa"), Optional.of("2048"));
        Map mykey = ipfs.name.publish(pointer.hash, Optional.of(name));
        String resolved = ipfs.name.resolve(Cid.decode((String) pub.get("Name")));
    }

    public void mountTest() throws IOException {
        Map mount = ipfs.mount(null, null);
    }

    @Test
    @Ignore("dhtTest may fail with timeout")
    public void dhtTest() throws IOException {
        MerkleNode raw = ipfs.block.put("Mathematics is wonderful".getBytes(), Optional.of("raw"));
//        Map get = ipfs.dht.get(raw.hash);
//        Map put = ipfs.dht.put("somekey", "somevalue");
        List<Map<String, Object>> findprovs = ipfs.dht.findprovs(raw.hash);
        List<Peer> peers = ipfs.swarm.peers();
        Map query = ipfs.dht.query(peers.get(0).id);
        Map find = ipfs.dht.findpeer(peers.get(0).id);
    }

    @Test
    public void localId() throws Exception {
        Map id = ipfs.id();
        System.out.println();
    }

    @Test
    public void statsTest() throws IOException {
        Map stats = ipfs.stats.bw();
        Map bitswap = ipfs.stats.bitswap(true);
        Map dht = ipfs.stats.dht();
        //{"Message":"can only return stats if Experimental.AcceleratedDHTClient is enabled","Code":0,"Type":"error"}
        //requires Map provide = ipfs.stats.provide();
        RepoStat repo = ipfs.stats.repo(false);
    }

    public void resolveTest() throws IOException {
        Multihash hash = Multihash.fromBase58("QmatmE9msSfkKxoffpHwNLNKgwZG8eT9Bud6YoPab52vpy");
        Map res = ipfs.resolve("ipns", hash, false);
    }

    @Test
    @Ignore
    public void swarmTest() throws IOException {
        Map<Multihash, List<MultiAddress>> addrs = ipfs.swarm.addrs();
        if (addrs.size() > 0) {
            boolean contacted = addrs.entrySet().stream()
                    .anyMatch(e -> {
                        Multihash target = e.getKey();
                        List<MultiAddress> nodeAddrs = e.getValue();
                        boolean contactable = nodeAddrs.stream()
                                .anyMatch(addr -> {
                                    try {
                                        MultiAddress peer = new MultiAddress(addr.toString() + "/ipfs/" + target.toBase58());
                                        Map connect = ipfs.swarm.connect(peer);
                                        Map disconnect = ipfs.swarm.disconnect(peer);
                                        return true;
                                    } catch (Exception ex) {
                                        return false;
                                    }
                                });
                        try {
                            Map id = ipfs.id(target);
                            Map ping = ipfs.ping(target);
                            return contactable;
                        } catch (Exception ex) {
                            // not all nodes have to be contactable
                            return false;
                        }
                    });
            if (!contacted)
                throw new IllegalStateException("Couldn't contact any node!");
        }
        List<Peer> peers = ipfs.swarm.peers();
    }

    @Test
    public void versionTest() throws IOException {
        Map listenAddrs = ipfs.version.versionDeps();
        System.currentTimeMillis();
    }

    @Test
    public void swarmTestFilters() throws IOException {
        Map listenAddrs = ipfs.swarm.listenAddrs();
        Map localAddrs = ipfs.swarm.localAddrs(true);
        String multiAddrFilter = "/ip4/192.168.0.0/ipcidr/16";
        Map rm = ipfs.swarm.rmFilter(multiAddrFilter);
        Map filters = ipfs.swarm.filters();
        List<String> filtersList = (List<String>)filters.get("Strings");
        Assert.assertNull("Filters empty", filtersList);

        Map added = ipfs.swarm.addFilter(multiAddrFilter);
        filters = ipfs.swarm.filters();
        filtersList = (List<String>)filters.get("Strings");
        Assert.assertFalse("Filters NOT empty", filtersList.isEmpty());
        rm = ipfs.swarm.rmFilter(multiAddrFilter);
    }

    @Test
    @Ignore
    public void swarmTestPeering() throws IOException {
        String id = "INSERT_VAL_HERE";
        Multihash hash = Multihash.fromBase58(id);
        String peer = "/ip6/::1/tcp/4001/p2p/" + id;
        MultiAddress ma = new MultiAddress(peer);
        Map addPeering = ipfs.swarm.addPeering(ma);
        Map lsPeering = ipfs.swarm.lsPeering();
        List<String> peeringList = (List<String>)lsPeering.get("Peers");
        Assert.assertFalse("Filters not empty", peeringList.isEmpty());
        Map rmPeering = ipfs.swarm.rmPeering(hash);
        lsPeering = ipfs.swarm.lsPeering();
        peeringList = (List<String>)lsPeering.get("Peers");
        Assert.assertTrue("Filters empty", peeringList.isEmpty());
    }

    @Test
    public void bitswapTest() throws IOException {
        List<Peer> peers = ipfs.swarm.peers();
        Map ledger = ipfs.bitswap.ledger(peers.get(0).id);
        Map want = ipfs.bitswap.wantlist(peers.get(0).id);
        //String reprovide = ipfs.bitswap.reprovide();
        Map stat = ipfs.bitswap.stat();
        Map stat2 = ipfs.bitswap.stat(true);
    }
    @Test
    public void bootstrapTest() throws IOException {
        List<MultiAddress> bootstrap = ipfs.bootstrap.list();
        List<MultiAddress> rm = ipfs.bootstrap.rm(bootstrap.get(0), false);
        List<MultiAddress> add = ipfs.bootstrap.add(bootstrap.get(0));
        List<MultiAddress> defaultPeers = ipfs.bootstrap.add();
        List<MultiAddress> peers = ipfs.bootstrap.list();
    }

    @Test
    public void cidTest() throws IOException {
        List<Map> bases = ipfs.cid.bases(true, true);
        List<Map> codecs = ipfs.cid.codecs(true, true);
        Map stat = ipfs.files.stat("/");
        String rootFolderHash = (String)stat.get("Hash");
        Map base32 = ipfs.cid.base32(Cid.decode(rootFolderHash));
        Map format = ipfs.cid.format(Cid.decode(rootFolderHash),
                Optional.of("%s"), Optional.of("1"),
                Optional.empty(), Optional.empty());

        List<Map> hashes = ipfs.cid.hashes(false, false);

        System.currentTimeMillis();
    }

    @Test
    public void diagTest() throws IOException {
        Map config = ipfs.config.show();
        Object api = ipfs.config.get("Addresses.API");
        Object val = ipfs.config.get("Datastore.GCPeriod");
        Map setResult = ipfs.config.set("Datastore.GCPeriod", val);
        ipfs.config.replace(new NamedStreamable.ByteArrayWrapper(JSONParser.toString(config).getBytes()));
//            Object log = ipfs.log();
        Map sys = ipfs.diag.sys();
        List<Map> cmds = ipfs.diag.cmds();
        String res = ipfs.diag.clearCmds();
        List<Map> cmds2 = ipfs.diag.cmds(true);
        //res = ipfs.diag.profile();
        //String profile = "default";
        //ipfs.config.profileApply(profile, true);
        //Map entry = ipfs.config("Addresses.API", Optional.of("/ip4/127.0.0.1/tcp/5001"), Optional.empty());
    }

    @Test
    public void toolsTest() throws IOException {
        String version = ipfs.version();
        int major = Integer.parseInt(version.split("\\.")[0]);
        int minor = Integer.parseInt(version.split("\\.")[1]);
        assertTrue(major >= 0 && minor >= 4);     // Requires at least 0.4.0
        Map commands = ipfs.commands();
    }

    @Test(expected = RuntimeException.class)
    public void testTimeoutFail() throws IOException {
        IPFS ipfs = new IPFS(new MultiAddress("/ip4/127.0.0.1/tcp/5001")).timeout(1000);
        ipfs.cat(Multihash.fromBase58("QmYpbSXyiCTYCbyMpzrQNix72nBYB8WRv6i39JqRc8C1ry"));
    }

    @Test
    public void testTimeoutOK() throws IOException {
        IPFS ipfs = new IPFS(new MultiAddress("/ip4/127.0.0.1/tcp/5001")).timeout(1000);
        ipfs.cat(Multihash.fromBase58("Qmaisz6NMhDB51cCvNWa1GMS7LU1pAxdF4Ld6Ft9kZEP2a"));
    }

    @Test
    public void addArgsTest() {
        AddArgs args = AddArgs.Builder.newInstance()
                .setInline()
                .setCidVersion(1)
                .build();
        String res = args.toString();
        assertEquals("args toString() format", "[cid-version = 1, inline = true]", res);
        String queryStr = args.toQueryString();
        assertEquals("args toQueryString() format", "inline=true&cid-version=1", queryStr);
    }

    // this api is disabled until deployment over IPFS is enabled
    public void updateTest() throws IOException {
        Object check = ipfs.update.check();
        Object update = ipfs.update();
    }

    private byte[] randomBytes(int len) {
        byte[] res = new byte[len];
        r.nextBytes(res);
        return res;
    }
}
