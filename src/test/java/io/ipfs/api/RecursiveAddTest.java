package io.ipfs.api;

import java.io.File;
import java.nio.file.*;
import java.util.*;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import io.ipfs.multiaddr.MultiAddress;

public class RecursiveAddTest {

    private final IPFS ipfs = new IPFS(new MultiAddress("/ip4/127.0.0.1/tcp/5001"));
    
    static File TMPDATA = new File("target/tmpdata");

    @BeforeClass
    public static void createTmpData() {
        TMPDATA.mkdirs();
    }

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder(TMPDATA);
    
    @Test
    public void testAdd() throws Exception {
        System.out.println("ipfs version: " + ipfs.version());

        String EXPECTED = "QmX5fZ6aUxNTAS7ZfYc8f4wPoMx6LctuNbMjuJZ9EmUSr6";

        Path base = tempFolder.newFolder().toPath();
        Files.write(base.resolve("index.html"), "<html></html>".getBytes());
        Path js = base.resolve("js");
        js.toFile().mkdirs();
        Files.write(js.resolve("func.js"), "function() {console.log('Hey');}".getBytes());

        List<MerkleNode> add = ipfs.add(new NamedStreamable.FileWrapper(base.toFile()));
        MerkleNode node = add.get(add.size() - 1);
        Assert.assertEquals(EXPECTED, node.hash.toBase58());
    }

    @Test
    public void binaryRecursiveAdd() throws Exception {
        String EXPECTED = "Qmd1dTx4Z1PHxSHDR9jYoyLJTrYsAau7zLPE3kqo14s84d";

        Path base = tempFolder.newFolder().toPath();
        base.toFile().mkdirs();
        byte[] bindata = new byte[1024*1024];
        new Random(28).nextBytes(bindata);
        Files.write(base.resolve("data.bin"), bindata);
        Path js = base.resolve("js");
        js.toFile().mkdirs();
        Files.write(js.resolve("func.js"), "function() {console.log('Hey');}".getBytes());

        List<MerkleNode> add = ipfs.add(new NamedStreamable.FileWrapper(base.toFile()));
        MerkleNode node = add.get(add.size() - 1);
        Assert.assertEquals(EXPECTED, node.hash.toBase58());
    }

    @Test
    public void largeBinaryRecursiveAdd() throws Exception {
        String EXPECTED = "QmZdfdj7nfxE68fBPUWAGrffGL3sYGx1MDEozMg73uD2wj";

        Path base = tempFolder.newFolder().toPath();
        base.toFile().mkdirs();
        byte[] bindata = new byte[100 * 1024*1024];
        new Random(28).nextBytes(bindata);
        Files.write(base.resolve("data.bin"), bindata);
        new Random(496).nextBytes(bindata);
        Files.write(base.resolve("data2.bin"), bindata);
        Path js = base.resolve("js");
        js.toFile().mkdirs();
        Files.write(js.resolve("func.js"), "function() {console.log('Hey');}".getBytes());

        List<MerkleNode> add = ipfs.add(new NamedStreamable.FileWrapper(base.toFile()));
        MerkleNode node = add.get(add.size() - 1);
        Assert.assertEquals(EXPECTED, node.hash.toBase58());
    }

    @Test
    public void largeBinaryInSubdirRecursiveAdd() throws Exception {
        String EXPECTED = "QmUYuMwCpgaxJhNxRA5Pmje8EfpEgU3eQSB9t3VngbxYJk";

        Path base = tempFolder.newFolder().toPath();
        base.toFile().mkdirs();
        Path bindir = base.resolve("moredata");
        bindir.toFile().mkdirs();
        byte[] bindata = new byte[100 * 1024*1024];
        new Random(28).nextBytes(bindata);
        Files.write(bindir.resolve("data.bin"), bindata);
        new Random(496).nextBytes(bindata);
        Files.write(bindir.resolve("data2.bin"), bindata);

        Path js = base.resolve("js");
        js.toFile().mkdirs();
        Files.write(js.resolve("func.js"), "function() {console.log('Hey');}".getBytes());

        List<MerkleNode> add = ipfs.add(new NamedStreamable.FileWrapper(base.toFile()));
        MerkleNode node = add.get(add.size() - 1);
        Assert.assertEquals(EXPECTED, node.hash.toBase58());
    }
}
