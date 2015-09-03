package org.ipfs;

import java.io.*;
import java.net.*;
import java.util.*;

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
    
    public static Object add(String host, int port, List<NamedStreamable> files) throws IOException {
        Multipart m = new Multipart("http://"+host+":"+port+"/api/v0/add?stream-channels=true", "UTF-8");
        for (NamedStreamable f: files)
            m.addFilePart("file", f);
        String res = m.finish();
        System.out.println(res);
        return JSONParser.parse(res);
    }

    public static Object ls(String host, int port, Hash hash) throws IOException {
        URL target = new URL("http", host, port, "/api/v0/ls/" + hash.toString());
        byte[] res = get(target);
        System.out.println(new String(res));
        return JSONParser.parse(new String(res));
    }

    public static Object cat(String host, int port, Hash hash) throws IOException {
        URL target = new URL("http", host, port, "/api/v0/cat/" + hash.toString());
        byte[] res = get(target);
        return new String(res);
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

        System.out.println(add("127.0.0.1", 5001, Arrays.asList(file1, file2, larger)));
        System.out.println(ls("127.0.0.1", 5001, new Hash("QmZULkCELmmk5XNfCgTnCyFgAVxBRBXyDHGGMVoLFLiXEN")));
        System.out.println(cat("127.0.0.1", 5001, new Hash("QmZULkCELmmk5XNfCgTnCyFgAVxBRBXyDHGGMVoLFLiXEN")));
        System.out.println(cat("127.0.0.1", 5001, new Hash("QmPJLkUykuWech5YJiaJhBVrc44kQPu1EpMK3KKfygvrbi")));
    }
}
