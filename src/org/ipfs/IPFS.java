package org.ipfs;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.*;

public class IPFS
{

    public static class Hash {
        private final String hash;

        public Hash(String hash) {
            this.hash = hash;
        }

        public String toString() {
            return hash;
        }
    }

    public enum Command {
        add,
        block,
        bootstrap,
        cat,
        commands,
        config,
        dht,
        diag,
        dns,
        get,
        id,
        log,
        ls,
        mount,
        name,
        object,
        pin,
        ping,
        refs,
        repo,
        resolve,
        stats,
        swarm,
        tour,
        file,
        update,
        version,
        bitswap
    }
    
    public static List<Map<String, Object>> add(String host, int port, List<NamedStreamable> files) throws IOException {
        Multipart m = new Multipart("http://"+host+":"+port+"/api/v0/add?stream-channels=true", "UTF-8");
        for (NamedStreamable f: files)
            m.addFilePart("file", f);
        String res = m.finish();
        return JSONParser.parseStream(res).stream().map(x -> (Map<String, Object>)x).collect(Collectors.toList());
    }

    public static Object ls(String host, int port, Hash hash) throws IOException {
        URL target = new URL("http", host, port, "/api/v0/ls/" + hash.toString());
        byte[] res = get(target);
        System.out.println(new String(res));
        return JSONParser.parse(new String(res));
    }

    public static byte[] cat(String host, int port, Hash hash) throws IOException {
        URL target = new URL("http", host, port, "/api/v0/cat/" + hash.toString());
        return get(target);
    }

    public static byte[] get(URL target) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) target.openConnection();
        conn.setRequestMethod("GET");

        InputStream in = conn.getInputStream();
        ByteArrayOutputStream resp = new ByteArrayOutputStream();

        byte[] buf = new byte[4096];
        int r;
        while ((r=in.read(buf)) >= 0)
            resp.write(buf, 0, r);
        return resp.toByteArray();
    }

    public static byte[] post(URL target, byte[] body, Map<String, String> headers) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) target.openConnection();
        for (String key: headers.keySet())
            conn.setRequestProperty(key, headers.get(key));
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        OutputStream out = conn.getOutputStream();
        out.write(body);
        out.flush();
        out.close();

        InputStream in = conn.getInputStream();
        ByteArrayOutputStream resp = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int r;
        while ((r=in.read(buf)) >= 0)
            resp.write(buf, 0, r);
        return resp.toByteArray();
    }

    public static void main(String[] a) throws Exception {
        NamedStreamable.ByteArrayWrapper file1 = new NamedStreamable.ByteArrayWrapper("hello.txt", "G'day world!".getBytes());
        NamedStreamable.ByteArrayWrapper file2 = new NamedStreamable.ByteArrayWrapper("Gday.txt", "G'day universe!".getBytes());
        byte[] largerData = new byte[100*1024*1024];
        new Random(1).nextBytes(largerData);
        NamedStreamable.ByteArrayWrapper larger = new NamedStreamable.ByteArrayWrapper("nontrivial.txt", largerData);

        List<NamedStreamable> inputFiles = Arrays.asList(file1, file2);
        List<Map<String, Object>> addResult = add("127.0.0.1", 5001, inputFiles);
        System.out.println(addResult);
        for (int i=0; i < addResult.size(); i++) {
            Map m = (Map) addResult.get(i);
            Object lsResult = ls("127.0.0.1", 5001, new Hash((String) m.get("Hash")));
            System.out.println(lsResult);
            byte[] catResult = cat("127.0.0.1", 5001, new Hash((String) m.get("Hash")));
            if (!new String(catResult).equals(new String(inputFiles.get(i).getContents())))
                throw new IllegalStateException("Different contents!");
            System.out.println(catResult);
        }
    }
}
