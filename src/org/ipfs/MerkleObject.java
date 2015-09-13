package org.ipfs;

import java.util.*;

public class MerkleObject {
    public final Optional<String> name;
    public final String hash;
    public final Optional<Integer> size;
    public final Optional<Integer> type;

    public MerkleObject(String hash) {
        this(Optional.empty(), hash);
    }

    public MerkleObject(Optional<String> name, String hash) {
        this(name, hash, Optional.empty(), Optional.empty());
    }

    public MerkleObject(Optional<String> name, String hash, Optional<Integer> size, Optional<Integer> type) {
        this.name = name;
        this.hash = hash;
        this.size = size;
        this.type = type;
    }

    @Override
    public boolean equals(Object b) {
        if (!(b instanceof MerkleObject))
            return false;
        MerkleObject other = (MerkleObject) b;
        return hash.equals(other.hash); // ignore name hash says it all
    }

    @Override
    public int hashCode() {
        return hash.hashCode();
    }

    public static MerkleObject fromJSON(Object json) {
        if (json instanceof String)
            return new MerkleObject((String)json);
        Map jsonMap = (Map)json;
        String hash = (String)jsonMap.get("Hash");
        Optional<String> name = jsonMap.containsKey("Name") ? Optional.of((String)jsonMap.get("Name")): Optional.empty();
        return new MerkleObject(name, hash);
    }
}
