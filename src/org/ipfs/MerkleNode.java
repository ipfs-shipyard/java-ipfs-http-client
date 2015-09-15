package org.ipfs;

import java.util.*;
import java.util.stream.*;

public class MerkleNode {
    public final Optional<String> name;
    public final String hash;
    public final Optional<Integer> size;
    public final Optional<Integer> type;
    public final List<MerkleNode> links;
    public final Optional<byte[]> data;

    public MerkleNode(String hash) {
        this(hash, Optional.empty());
    }

    public MerkleNode(String hash, Optional<String> name) {
        this(hash, name, Optional.empty(), Optional.empty(), Arrays.asList(), Optional.empty());
    }

    public MerkleNode(String hash, Optional<String> name, Optional<Integer> size, Optional<Integer> type, List<MerkleNode> links, Optional<byte[]> data) {
        this.name = name;
        this.hash = hash;
        this.size = size;
        this.type = type;
        this.links = links;
        this.data = data;
    }

    @Override
    public boolean equals(Object b) {
        if (!(b instanceof MerkleNode))
            return false;
        MerkleNode other = (MerkleNode) b;
        return hash.equals(other.hash); // ignore name hash says it all
    }

    @Override
    public int hashCode() {
        return hash.hashCode();
    }

    public static MerkleNode fromJSON(Object rawjson) {
        if (rawjson instanceof String)
            return new MerkleNode((String)rawjson);
        Map json = (Map)rawjson;
        String hash = (String)json.get("Hash");
        if (hash == null)
            hash = (String)json.get("Key");
        Optional<String> name = json.containsKey("Name") ? Optional.of((String)json.get("Name")): Optional.empty();
        Optional<Integer> size = json.containsKey("Size") ? Optional.<Integer>empty().of((Integer) json.get("Size")): Optional.<Integer>empty().empty();
        Optional<Integer> type = json.containsKey("Type") ? Optional.<Integer>empty().of((Integer) json.get("Type")): Optional.<Integer>empty().empty();
        List<MerkleNode> links = json.containsKey("Links") ? ((List<Object>)json.get("Links")).stream().map(x -> MerkleNode.fromJSON(x)).collect(Collectors.toList()) : Arrays.asList();
        Optional<byte[]> data = json.containsKey("Data") ? Optional.of(((String)json.get("Data")).getBytes()): Optional.empty();
        return new MerkleNode(hash, name, size, type, links, data);
    }
}
