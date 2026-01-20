package io.ipfs.api;

import io.ipfs.cid.*;
import io.ipfs.multiaddr.*;
import io.ipfs.multibase.Base58;
import io.ipfs.multihash.*;

import java.util.*;
import java.util.function.*;

public class Peer {
    public final MultiAddress address;
    public final Cid id;
    public final long latency;
    public final String muxer;
    public final Object streams;

    public Peer(MultiAddress address, Cid id, long latency, String muxer, Object streams) {
        this.address = address;
        this.id = id;
        this.latency = latency;
        this.muxer = muxer;
        this.streams = streams;
    }

    public static Peer fromJSON(Object json) {
        if (! (json instanceof Map))
            throw new IllegalStateException("Incorrect json for Peer: " + JSONParser.toString(json));
        Map m = (Map) json;
        Function<String, String> val = key -> (String) m.get(key);
        Cid peer = decodePeerId(val.apply("Peer"));
        long latency = m.containsKey("Latency") ? Long.parseLong(val.apply("Latency")) : -1;
        return new Peer(new MultiAddress(val.apply("Addr")), peer, latency, val.apply("Muxer"), val.apply("Streams"));
    }

    // See https://github.com/Peergos/Peergos/blob/81064fdb2cdf6b6fe126cf6a20d4d40ecd148938/src/peergos/shared/io/ipfs/Cid.java#L148
    public static Cid decodePeerId(String peerId) {
        if (peerId.startsWith("1")) {
            // convert base58 encoded identity multihash to cidV1
            Multihash hash = Multihash.deserialize(Base58.decode(peerId));
            return new Cid(1, Cid.Codec.Libp2pKey, hash.getType(), hash.getHash());
        }
        return Cid.decode(peerId);
    }
    @Override
    public String toString() {
        return id + "@" + address;
    }
}
