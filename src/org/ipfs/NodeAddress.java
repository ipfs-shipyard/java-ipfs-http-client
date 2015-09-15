package org.ipfs;

public class NodeAddress
{
    public final String address;

    public NodeAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return address;
    }
}
