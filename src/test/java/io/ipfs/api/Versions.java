package io.ipfs.api;

import org.junit.*;

import java.util.*;
import java.util.stream.*;

public class Versions {

    @Test
    public void sorting(){
        List<String> original = Arrays.asList("1.0.3", "0.4.9", "0.4.10", "0.5.1-rc1", "0.5.1-rc2", "0.5.1-rc2+meta");
        List<Version> versions = original.stream().map(Version::parse).collect(Collectors.toList());
        Collections.sort(versions);
        List<String> sorted = versions.stream().map(Object::toString).collect(Collectors.toList());
        List<String> correct = Arrays.asList("0.4.9", "0.4.10", "0.5.1-rc1", "0.5.1-rc2", "0.5.1-rc2+meta", "1.0.3");
        Assert.assertTrue("Correct version sorting", sorted.equals(correct));
    }
}
