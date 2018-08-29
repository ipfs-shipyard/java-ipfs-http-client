package io.ipfs.api;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import io.ipfs.api.NamedStreamable.FileWrapper;
import io.ipfs.multiaddr.MultiAddress;

/**
 *
 * ipfs daemon --enable-pubsub-experiment &
 *
 * ipfs pin rm `ipfs pin ls -qt recursive`
 *
 * ipfs --api=/ip4/127.0.0.1/tcp/5001 add -r src/test/resources/html
 *
 */
public class SimpleAddTest {

    static final Map<String, String> cids = new LinkedHashMap<>();
    static {
        cids.put("index.html", "QmVts3YjmhsCSqMv8Thk1CCy1nnpCbqEFjbkjS7PEzthZE");
        cids.put("html", "QmUQvDumYa8najL94EnGhmGobyMyNzAmCSpfAxYnYcQHZD");
    }

    IPFS ipfs = new IPFS(new MultiAddress("/ip4/127.0.0.1/tcp/5001"));

    @Test
    public void testSingle() throws Exception {
        Path path = Paths.get("src/test/resources/html/index.html");
        NamedStreamable file = new FileWrapper(path.toFile());
        List<MerkleNode> tree = ipfs.add(file);

        Assert.assertEquals(1, tree.size());
        Assert.assertEquals("index.html", tree.get(0).name.get());
        Assert.assertEquals(cids.get("index.html"), tree.get(0).hash.toBase58());
    }

    @Test
    public void testSingleWrapped() throws Exception {

        Path path = Paths.get("src/test/resources/html/index.html");
        NamedStreamable file = new FileWrapper(path.toFile());
        List<MerkleNode> tree = ipfs.add(file, true);

        Assert.assertEquals(2, tree.size());
        Assert.assertEquals("index.html", tree.get(0).name.get());
        Assert.assertEquals(cids.get("index.html"), tree.get(0).hash.toBase58());
    }

    @Test
    public void testSingleOnlyHash() throws Exception {

        Path path = Paths.get("src/test/resources/html/index.html");
        NamedStreamable file = new FileWrapper(path.toFile());
        List<MerkleNode> tree = ipfs.add(file, false, true);

        Assert.assertEquals(1, tree.size());
        Assert.assertEquals("index.html", tree.get(0).name.get());
        Assert.assertEquals(cids.get("index.html"), tree.get(0).hash.toBase58());
    }

    @Test
    public void testRecursive() throws Exception {

        Path path = Paths.get("src/test/resources/html");
        NamedStreamable file = new FileWrapper(path.toFile());
        List<MerkleNode> tree = ipfs.add(file);

        Assert.assertEquals(8, tree.size());
        Assert.assertEquals("html", tree.get(7).name.get());
        Assert.assertEquals(cids.get("html"), tree.get(7).hash.toBase58());
    }

    @Test
    public void testRecursiveOnlyHash() throws Exception {

        Path path = Paths.get("src/test/resources/html");
        NamedStreamable file = new FileWrapper(path.toFile());
        List<MerkleNode> tree = ipfs.add(file, false, true);

        Assert.assertEquals(8, tree.size());
        Assert.assertEquals("html", tree.get(7).name.get());
        Assert.assertEquals(cids.get("html"), tree.get(7).hash.toBase58());
    }
}
