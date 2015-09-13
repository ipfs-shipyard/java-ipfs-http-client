package org.ipfs;

import java.util.*;
import java.util.stream.*;

public class MerkleNode {
    public final Hash hash;
    public final List<Hash> links;

    public MerkleNode(Hash hash, List<Hash> links) {
        this.hash = hash;
        this.links = links;
    }

    public static MerkleNode fromJSON(Map json) {
        Hash hash = new Hash((String)json.get("Hash"));
        List<Hash> links = ((List<Object>)json.get("Links")).stream().map(x -> new Hash((String)x)).collect(Collectors.toList());
        return new MerkleNode(hash, links);
    }
}
