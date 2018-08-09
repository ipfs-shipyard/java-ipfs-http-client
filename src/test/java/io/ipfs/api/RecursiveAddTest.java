package io.ipfs.api;

import java.io.*;
import java.nio.file.*;

import org.junit.Assert;
import org.junit.Test;

import io.ipfs.multiaddr.MultiAddress;

public class RecursiveAddTest {

    private final IPFS ipfs = new IPFS(new MultiAddress("/ip4/127.0.0.1/tcp/5001"));
    
    @Test
    public void testAdd() throws Exception {
        
        String TEST_HASH = "QmWVMRbFTrA6pMhsfXzsyoWqmrhy5FPd4VitfX79R5bguw";
        
        System.out.println("ipfs version: " + ipfs.version());
        System.out.println("pwd: " + new File("").getAbsolutePath());
        
        Path path = Paths.get("src/test/resources/html");
        MerkleNode node = ipfs.add(new NamedStreamable.FileWrapper(path.toFile())).get(0);
        Assert.assertEquals(TEST_HASH, node.hash.toBase58());
    }
}
