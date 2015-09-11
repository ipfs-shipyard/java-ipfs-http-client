package org.ipfs;

import java.io.*;

public interface NamedStreamable
{
    InputStream getInputStream() throws IOException;

    String getName();

    default byte[] getContents() throws IOException {
        InputStream in = getInputStream();
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] tmp = new byte[4096];
        int r;
        while ((r=in.read(tmp))>= 0)
            bout.write(tmp, 0, r);
        return bout.toByteArray();
    }

    class FileWrapper implements NamedStreamable {
        private final File source;

        public FileWrapper(File source) {
            this.source = source;
        }

        public InputStream getInputStream() throws IOException {
            return new FileInputStream(source);
        }

        public String getName() {
            return source.getName();
        }
    }

    class ByteArrayWrapper implements NamedStreamable {
        private final String name;
        private final byte[] data;

        public ByteArrayWrapper(String name, byte[] data) {
            this.name = name;
            this.data = data;
        }

        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(data);
        }

        public String getName() {
            return name;
        }
    }
}
