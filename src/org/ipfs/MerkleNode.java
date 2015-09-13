package org.ipfs;

import java.util.*;
import java.util.stream.*;

public class MerkleNode {
    public final MerkleObject merkleObject;
    public final List<MerkleObject> links;

    public MerkleNode(MerkleObject merkleObject, List<MerkleObject> links) {
        this.merkleObject = merkleObject;
        this.links = links;
    }

    public static MerkleNode fromJSON(Map json) {
        MerkleObject merkleObject = new MerkleObject((String)json.get("Hash"));
        List<MerkleObject> links = ((List<Object>)json.get("Links")).stream().map(x -> MerkleObject.fromJSON(x)).collect(Collectors.toList());
        return new MerkleNode(merkleObject, links);
    }
}
