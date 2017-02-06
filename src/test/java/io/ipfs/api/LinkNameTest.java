package io.ipfs.api;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import io.ipfs.multiaddr.MultiAddress;
import io.ipfs.multihash.Multihash;

public class LinkNameTest {
	
    private final IPFS ipfs = new IPFS(new MultiAddress("/ip4/127.0.0.1/tcp/5001"));


	@Test
	public void test() throws IOException {
        Multihash EMPTY = ipfs.object._new(Optional.empty()).hash;
        MerkleNode data = ipfs.object.patch(EMPTY, "set-data", Optional.of("childdata".getBytes()), Optional.empty(), Optional.empty());
        Multihash child = data.hash;

        MerkleNode tmp1 = ipfs.object.patch(EMPTY, "set-data", Optional.of("parent1_data".getBytes()), Optional.empty(), Optional.empty());
        String linkName = "linkname";
		Multihash parent = ipfs.object.patch(tmp1.hash, "add-link", Optional.empty(), Optional.of(linkName), Optional.of(child)).hash;
        
        MerkleNode parentNode = ipfs.object.get(parent);
        assertEquals(linkName, parentNode.links.get(0).name.get());
        Map<String, Object> json = (Map<String, Object>) parentNode.toJSON();
		List<Object> links = (List<Object>) json.get("Links");
		Map<String, Object> firstLink = (Map<String, Object>) links.get(0);
		assertEquals(linkName, firstLink.get("Name"));
	}

}
