package org.ipfs;

import java.io.*;
import java.util.*;

public class Test {

    @org.junit.Test
    public void singleFileTest() {
        try {
            NamedStreamable.ByteArrayWrapper file1 = new NamedStreamable.ByteArrayWrapper("hello.txt", "G'day world! IPFS rocks!".getBytes());
            List<NamedStreamable> inputFiles = Arrays.asList(file1);
            IPFS ipfs = new IPFS("127.0.0.1", 5001);
            List<Hash> addResult = ipfs.add(inputFiles);
            System.out.println(addResult);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] a) throws Exception {
        NamedStreamable.ByteArrayWrapper file1 = new NamedStreamable.ByteArrayWrapper("hello.txt", "G'day world! IPFS rocks!".getBytes());
        NamedStreamable.ByteArrayWrapper file2 = new NamedStreamable.ByteArrayWrapper("Gday.txt", "G'day universe! IPFS rocks!".getBytes());
        byte[] largerData = new byte[100*1024*1024];
        new Random(1).nextBytes(largerData);
        NamedStreamable.ByteArrayWrapper larger = new NamedStreamable.ByteArrayWrapper("nontrivial.txt", largerData);

        List<NamedStreamable> inputFiles = Arrays.asList(file1, file2);

        IPFS ipfs = new IPFS("127.0.0.1", 5001);
        List<Hash> addResult = ipfs.add(inputFiles);
        System.out.println(addResult);
        for (int i=0; i < addResult.size(); i++) {
            Hash hash = addResult.get(i);
            Object lsResult = ipfs.ls(hash);
            System.out.println(lsResult);
            byte[] catResult = ipfs.cat(hash);
            if (!new String(catResult).equals(new String(inputFiles.get(i).getContents())))
                throw new IllegalStateException("Different contents!");
            System.out.println("File contents: " + new String(catResult));

//            Object pinAddResult = ipfs.pinAdd(hash);
            Object pinLsResult = ipfs.pinLs();
            Object pinRmResult = ipfs.pinRm(hash);
        }
    }
}
