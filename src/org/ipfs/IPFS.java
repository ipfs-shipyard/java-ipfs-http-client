package org.ipfs;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.*;

public class IPFS {

    public List<String> ObjectTemplates = Arrays.asList("unixfs-dir");

    public final String host;
    public final int port;
    private final String version;
    public final Pin pin = new Pin();
    public final Repo repo = new Repo();
    public final IPFSObject object = new IPFSObject();
    public final Swarm swarm = new Swarm();
    public final Block block = new Block();
    public final Diag diag = new Diag();
    public final Config config = new Config();
    public final Refs refs = new Refs();
    public final Update update = new Update();
    public final DHT dht = new DHT();
    public final File file = new File();
    public final Stats stats = new Stats();
    public final Name name = new Name();

    public IPFS(String host, int port) {
        this(host, port, "/api/v0/");
    }

    public IPFS(String host, int port, String version) {
        this.host = host;
        this.port = port;
        this.version = version;
    }

    public MerkleNode add(NamedStreamable file) throws IOException {
        return add(Arrays.<NamedStreamable>asList(file)).get(0);
    }

    public List<MerkleNode> add(List<NamedStreamable> files) throws IOException {
        Multipart m = new Multipart("http://" + host + ":" + port + version+"add?stream-channels=true", "UTF-8");
        for (NamedStreamable f : files)
            m.addFilePart("file", f);
        String res = m.finish();
        return JSONParser.parseStream(res).stream().map(x -> MerkleNode.fromJSON((Map<String, Object>) x)).collect(Collectors.toList());
    }

    public List<MerkleNode> ls(MerkleNode merkleObject) throws IOException {
        Map res = retrieveMap("ls/" + merkleObject.hash);
        return ((List<Object>) res.get("Objects")).stream().map(x -> MerkleNode.fromJSON((Map) x)).collect(Collectors.toList());
    }

    public byte[] cat(MerkleNode merkleObject) throws IOException {
        return retrieve("cat/" + merkleObject.hash);
    }

    public byte[] get(MerkleNode merkleObject) throws IOException {
        return retrieve("get/" + merkleObject.hash);
    }

    public Map refs(String hash, boolean recursive) throws IOException {
        return retrieveMap("refs?arg=" + hash +"&r="+recursive);
    }

    public Map resolve(String scheme, String hash, boolean recursive) throws IOException {
        return retrieveMap("resolve?arg=/" + scheme+"/"+hash +"&r="+recursive);
    }


    public String dns(String domain) throws IOException {
        Map res = retrieveMap("dns?arg=" + domain);
        return (String)res.get("Path");
    }

    public Map mount(java.io.File ipfsRoot, java.io.File ipnsRoot) throws IOException {
        if (ipfsRoot != null && !ipfsRoot.exists())
            ipfsRoot.mkdirs();
        if (ipnsRoot != null && !ipnsRoot.exists())
            ipnsRoot.mkdirs();
        return (Map)retrieveAndParse("mount?arg=" + (ipfsRoot != null ? ipfsRoot.getPath() : "/ipfs" ) + "&arg=" +
                (ipnsRoot != null ? ipnsRoot.getPath() : "/ipns" ));
    }

    // level 2 commands
    class Refs {
        public List<String> local() throws IOException {
            return Arrays.asList(new String(retrieve("refs/local")).split("\n"));
        }
    }

    /* Pinning an object ensures a local copy of it is kept.
     */
    class Pin {
        public Object add(MerkleNode merkleObject) throws IOException {
            return retrieveAndParse("pin/add?stream-channels=true&arg=" + merkleObject.hash);
        }

        public Object ls() throws IOException {
            return retrieveAndParse("pin/ls?stream-channels=true");
        }

        public List<MerkleNode> rm(MerkleNode merkleObject, boolean recursive) throws IOException {
            Map json = retrieveMap("pin/rm?stream-channels=true&r=" + recursive + "&arg=" + merkleObject.hash);
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

        public Map stat(MerkleNode merkleObject) throws IOException {
            return retrieveMap("block/stat?stream-channels=true&arg=" + merkleObject.hash);
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
            Map json = retrieveMap("object/get?stream-channels=true&arg=" + merkleObject.hash);
            json.put("Hash", merkleObject.hash.toBase58());
            return MerkleNode.fromJSON(json);
        }

        public MerkleNode links(MerkleNode merkleObject) throws IOException {
            Map json = retrieveMap("object/links?stream-channels=true&arg=" + merkleObject.hash);
            return MerkleNode.fromJSON(json);
        }

        public Map<String, Object> stat(MerkleNode merkleObject) throws IOException {
            return retrieveMap("object/stat?stream-channels=true&arg=" + merkleObject.hash);
        }

        public byte[] data(MerkleNode merkleObject) throws IOException {
            return retrieve("object/data?stream-channels=true&arg=" + merkleObject.hash);
        }

        public MerkleNode _new(Optional<String> template) throws IOException {
            if (template.isPresent() && !ObjectTemplates.contains(template.get()))
                throw new IllegalStateException("Unrecognised template: "+template.get());
            Map json = retrieveMap("object/new?stream-channels=true"+(template.isPresent() ? "&arg=" + template.get() : ""));
            return MerkleNode.fromJSON(json);
        }

        // TODO patch
    }

    class Name {
        public Map publish(MerkleNode node) throws IOException {
            return publish(Optional.empty(), node);
        }

        public Map publish(Optional<String> id, MerkleNode node) throws IOException {
            return retrieveMap("name/publish?arg=" + (id.isPresent() ? id+"&arg=" : "") + "/ipfs/"+node.hash);
        }

        public String resolve(String addr) throws IOException {
            Map res = (Map) retrieveAndParse("name/resolve?arg=" + addr);
            return (String)res.get("Path");
        }
    }

    class DHT {
        public Map findprovs(MerkleNode node) throws IOException {
            return retrieveMap("dht/findprovs?arg=" + node.hash);
        }

        public Map query(NodeAddress addr) throws IOException {
            return retrieveMap("dht/query?arg=" + addr.address);
        }

        public Map findpeer(NodeAddress addr) throws IOException {
            return retrieveMap("dht/findpeer?arg=" + addr.address);
        }

        public Map get(MerkleNode node) throws IOException {
            return retrieveMap("dht/get?arg=" + node.hash);
        }

        public Map put(String key, String value) throws IOException {
            return retrieveMap("dht/put?arg=" + key + "&arg="+value);
        }
    }

    class File {
        public Map ls(String path) throws IOException {
            return retrieveMap("file/ls?arg=" + path);
        }
    }

    // Network commands

    public Map bootstrap() throws IOException {
        return retrieveMap("bootstrap/");
    }

    /*  ipfs swarm is a tool to manipulate the network swarm. The swarm is the
        component that opens, listens for, and maintains connections to other
        ipfs peers in the internet.
     */
    class Swarm {
        public List<NodeAddress> peers() throws IOException {
            Map m = retrieveMap("swarm/peers?stream-channels=true");
            return ((List<Object>)m.get("Strings")).stream().map(x -> new NodeAddress((String)x)).collect(Collectors.toList());
        }

        public Map addrs() throws IOException {
            Map m = retrieveMap("swarm/addrs?stream-channels=true");
            return (Map<String, Object>)m.get("Addrs");
        }

        // TODO connect, disconnect
    }

    class Diag {
        public String net() throws IOException {
            return new String(retrieve("diag/net?stream-channels=true"));
        }
    }

    public Map ping(String target) throws IOException {
        return retrieveMap("ping/" + target.toString());
    }

    public Map id(String target) throws IOException {
        return retrieveMap("id/" + target.toString());
    }

    class Stats {
        public Map bw() throws IOException {
            return retrieveMap("stats/bw");
        }
    }

    // Tools
    public String version() throws IOException {
        Map m = (Map)retrieveAndParse("version");
        return (String)m.get("Version");
    }

    public Map commands() throws IOException {
        return retrieveMap("commands");
    }

    public Map log() throws IOException {
        return retrieveMap("log/tail");
    }

    class Config {
        public Map show() throws IOException {
            return (Map)retrieveAndParse("config/show");
        }

        public void replace(NamedStreamable file) throws IOException {
            Multipart m = new Multipart("http://" + host + ":" + port + version+"config/replace?stream-channels=true", "UTF-8");
            m.addFilePart("file", file);
            String res = m.finish();
        }

        public String get(String key) throws IOException {
            Map m = (Map)retrieveAndParse("config?arg="+key);
            return (String)m.get("Value");
        }

        public Map set(String key, String value) throws IOException {
            return retrieveMap("config?arg=" + key + "&arg=" + value);
        }
    }

    public Object update() throws IOException {
        return retrieveAndParse("update");
    }

    class Update {
        public Object check() throws IOException {
            return retrieveAndParse("update/check");
        }

        public Object log() throws IOException {
            return retrieveAndParse("update/log");
        }
    }

    private Map retrieveMap(String path) throws IOException {
        return (Map)retrieveAndParse(path);
    }

    private Object retrieveAndParse(String path) throws IOException {
        byte[] res = retrieve(path);
        return JSONParser.parse(new String(res));
    }

    private byte[] retrieve(String path) throws IOException {
        URL target = new URL("http", host, port, version + path);
        return IPFS.get(target);
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
