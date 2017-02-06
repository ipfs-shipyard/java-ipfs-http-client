package io.ipfs.api;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Map;

import org.junit.Test;

import io.ipfs.multiaddr.MultiAddress;

public class IdTest {

    private final IPFS ipfs = new IPFS(new MultiAddress("/ip4/127.0.0.1/tcp/5001"));

	@Test
	public void idWithoutParamsReturnsOurInfo() throws IOException {
		Map idMap = ipfs.id();
		assertTrue(idMap.containsKey("PublicKey"));
	}

	@Test
	public void idWithParamReturnsPeerInfo() throws IOException {
		Map<String,String> myIdMap = ipfs.id();
		String myId = myIdMap.get("ID");
		String myKey = myIdMap.get("PublicKey");
		assertEquals(ipfs.id(myId).get("PublicKey"), myKey);
	}

}
