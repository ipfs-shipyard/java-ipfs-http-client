module java.ipfs.http.client {
  requires io.ipfs.multibase;
  requires io.ipfs.multihash;
  requires io.ipfs.multiaddr;
  requires io.ipfs.cid;

  exports io.ipfs.api;
  exports io.ipfs.api.cbor;
}
