package org.ipfs;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.*;

public class IPFS {

    public enum Command {
        // TODO
        config,
        dht,
        diag,
        dns,
        get,
        mount,
        name,
        refs,
        resolve,
        stats,
        tour,
        file,
        update,
        bitswap
    }

    public final String host;
    public final int port;
    private final String version;
    public final Pin pin = new Pin();
    public final Repo repo = new Repo();
    public final IPFSObject object = new IPFSObject();
    public final Swarm swarm = new Swarm();
    public final Block block = new Block();
    public final Diag diag = new Diag();
    public final Name name = new Name();

    public IPFS(String host, int port) {
        this(host, port, "/api/v0/");
    }

    public IPFS(String host, int port, String version) {
        this.host = host;
        this.port = port;
        this.version = version;
    }

    public List<MerkleNode> add(List<NamedStreamable> files) throws IOException {
        Multipart m = new Multipart("http://" + host + ":" + port + version+"add?stream-channels=true", "UTF-8");
        for (NamedStreamable f : files)
            m.addFilePart("file", f);
        String res = m.finish();
        return JSONParser.parseStream(res).stream().map(x -> MerkleNode.fromJSON((Map<String, Object>) x)).collect(Collectors.toList());
    }

    public List<MerkleNode> ls(MerkleNode merkleObject) throws IOException {
        Map res = (Map) retrieveAndParse("ls/" + merkleObject.hash);
        return ((List<Object>) res.get("Objects")).stream().map(x -> MerkleNode.fromJSON((Map) x)).collect(Collectors.toList());
    }

    public byte[] cat(MerkleNode merkleObject) throws IOException {
        return retrieve("cat/" + merkleObject.hash);
    }

    // level 2 commands

    /* Pinning an object ensure a local copy of it is kept.
     */
    class Pin {
        public Object add(MerkleNode merkleObject) throws IOException {
            return retrieveAndParse("pin/add?stream-channels=true&arg=" + merkleObject.hash);
        }

        public Object ls() throws IOException {
            return retrieveAndParse("pin/ls?stream-channels=true");
        }

        public List<MerkleNode> rm(MerkleNode merkleObject, boolean recursive) throws IOException {
            Map json = (Map) retrieveAndParse("pin/rm?stream-channels=true&r=" + recursive + "&arg=" + merkleObject.hash);
            return ((List<Object>) json.get("Pinned")).stream().map(x -> new MerkleNode((String) x)).collect(Collectors.toList());
        }
    }

    /* 'ipfs repo' is a plumbing command used to manipulate the repo.
     */
    class Repo {
        public Object gc() throws IOException {
            return retrieveAndParse("repo/gc");
        }
    }

    /* 'ipfs block' is a plumbing command used to manipulate raw ipfs blocks.
     */
    class Block {
        public byte[] get(MerkleNode merkleObject) throws IOException {
            return retrieve("block/get?stream-channels=true&arg=" + merkleObject.hash);
        }

        public List<MerkleNode> put(List<byte[]> data) throws IOException {
            Multipart m = new Multipart("http://" + host + ":" + port + version+"block/put?stream-channels=true", "UTF-8");
            for (byte[] f : data)
                m.addFilePart("file", new NamedStreamable.ByteArrayWrapper(f));
            String res = m.finish();
            return JSONParser.parseStream(res).stream().map(x -> MerkleNode.fromJSON((Map<String, Object>) x)).collect(Collectors.toList());
        }
    }

    /* 'ipfs object' is a plumbing command used to manipulate DAG objects directly. {Object} is a subset of {Block}
     */
    class IPFSObject {
        public List<MerkleNode> put(List<byte[]> data) throws IOException {
            Multipart m = new Multipart("http://" + host + ":" + port + version+"object/put?stream-channels=true", "UTF-8");
            for (byte[] f : data)
                m.addFilePart("file", new NamedStreamable.ByteArrayWrapper(f));
            String res = m.finish();
            return JSONParser.parseStream(res).stream().map(x -> MerkleNode.fromJSON((Map<String, Object>) x)).collect(Collectors.toList());
        }

        public MerkleNode get(MerkleNode merkleObject) throws IOException {
            Map json = (Map)retrieveAndParse("object/get?stream-channels=true&arg=" + merkleObject.hash);
            json.put("Hash", merkleObject.hash);
            return MerkleNode.fromJSON(json);
        }

        public MerkleNode links(MerkleNode merkleObject) throws IOException {
            Map json = (Map)retrieveAndParse("object/links?stream-channels=true&arg=" + merkleObject.hash);
            return MerkleNode.fromJSON(json);
        }

        public Map<String, Object> stat(MerkleNode merkleObject) throws IOException {
            return (Map)retrieveAndParse("object/stat?stream-channels=true&arg=" + merkleObject.hash);
        }

        public byte[] data(MerkleNode merkleObject) throws IOException {
            return retrieve("object/data?stream-channels=true&arg=" + merkleObject.hash);
        }

        // TODO new, patch
    }

    class Name {
        // publish

        // resolve
    }


    // Network commands

    public Map bootstrap() throws IOException {
        return (Map)retrieveAndParse("bootstrap/");
    }

    /*  ipfs swarm is a tool to manipulate the network swarm. The swarm is the
        component that opens, listens for, and maintains connections to other
        ipfs peers in the internet.
     */
    class Swarm {
        public List<NodeAddress> peers() throws IOException {
            Map m = (Map)retrieveAndParse("swarm/peers?stream-channels=true");
            return ((List<Object>)m.get("Strings")).stream().map(x -> new NodeAddress((String)x)).collect(Collectors.toList());
        }

        public Map<String, Object> addrs() throws IOException {
            Map m = (Map)retrieveAndParse("swarm/addrs?stream-channels=true");
            return (Map<String, Object>)m.get("Addrs");
        }
    }

    class Diag {
        public String net() throws IOException {
            return new String(retrieve("diag/net?stream-channels=true"));
        }
    }

    public Map ping(String target) throws IOException {
        return (Map)retrieveAndParse("ping/"+target.toString());
    }

    public Map id(String target) throws IOException {
        return (Map)retrieveAndParse("id/"+target.toString());
    }

    // Tools
    public String version() throws IOException {
        Map m = (Map)retrieveAndParse("version");
        return (String)m.get("Version");
    }

    public Map commands() throws IOException {
        return (Map)retrieveAndParse("commands");
    }

    public Map log() throws IOException {
        return (Map)retrieveAndParse("log/tail");
    }

    private Object retrieveAndParse(String path) throws IOException {
        byte[] res = retrieve(path);
        return JSONParser.parse(new String(res));
    }

    private byte[] retrieve(String path) throws IOException {
        URL target = new URL("http", host, port, version + path);
        return IPFS.get(target);
    }

    private Object retrieveAndParsePost(String path, byte[] body) throws IOException {
        URL target = new URL("http", host, port, version + path);
        byte[] res = post(target, body, Collections.EMPTY_MAP);
        return JSONParser.parse(new String(res));
    }

    private static byte[] get(URL target) throws IOException {
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

    private static byte[] post(URL target, byte[] body, Map<String, String> headers) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) target.openConnection();
        for (String key: headers.keySet())
            conn.setRequestProperty(key, headers.get(key));
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
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
