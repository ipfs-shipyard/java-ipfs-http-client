package org.ipfs;

import java.util.*;

public class Hash {
    public final Optional<String> name;
    public final String hash;

    public Hash(String hash) {
        this.name = Optional.empty();
        this.hash = hash;
    }

    public Hash(Optional<String> name, String hash) {
        this.name = name;
        this.hash = hash;
    }

    @Override
    public boolean equals(Object b) {
        if (!(b instanceof Hash))
            return false;
        Hash other = (Hash) b;
        return hash.equals(other.hash) && name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return hash.hashCode() ^ name.hashCode();
    }

    public static Hash fromJSON(Map json) {
        String hash = (String)json.get("Hash");
        Optional<String> name = json.containsKey("Name") ? Optional.of((String)json.get("Name")): Optional.empty();
        return new Hash(name, hash);
    }
}
