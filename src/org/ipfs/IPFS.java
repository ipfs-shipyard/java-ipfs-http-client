package org.ipfs;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.*;

public class IPFS
{

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

    public final String host;
    public final int port;

    public IPFS(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public List<Hash> add(List<NamedStreamable> files) throws IOException {
        Multipart m = new Multipart("http://"+host+":"+port+"/api/v0/add?stream-channels=true", "UTF-8");
        for (NamedStreamable f: files)
            m.addFilePart("file", f);
        String res = m.finish();
        return JSONParser.parseStream(res).stream().map(x -> Hash.fromJSON((Map<String, Object>)x)).collect(Collectors.toList());
    }

    public Object pinAdd(Hash hash) throws IOException {
        URL target = new URL("http", host, port, "/api/v0/pin/add?stream-channels=true&arg=" + hash.hash);
        byte[] res = get(target);
        return JSONParser.parse(new String(res));
    }

    public Object pinLs() throws IOException {
        URL target = new URL("http", host, port, "/api/v0/pin/ls?stream-channels=true");
        byte[] res = get(target);
        return JSONParser.parse(new String(res));
    }

    public Object pinRm(Hash hash) throws IOException {
        URL target = new URL("http", host, port, "/api/v0/pin/rm?stream-channels=true&arg=" + hash.hash);
        byte[] res = get(target);
        return JSONParser.parse(new String(res));
    }

    public Object ls(Hash hash) throws IOException {
        URL target = new URL("http", host, port, "/api/v0/ls/" + hash.hash);
        byte[] res = get(target);
        return JSONParser.parse(new String(res));
    }

    public byte[] cat(Hash hash) throws IOException {
        URL target = new URL("http", host, port, "/api/v0/cat/" + hash.hash);
        return get(target);
    }

    public static byte[] get(URL target) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) target.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-Type", "application/json");

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
}
