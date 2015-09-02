package org.ipfs;

import java.io.*;

public interface NamedStreamable
{
    InputStream getInputStream() throws IOException;

    String getName();

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
