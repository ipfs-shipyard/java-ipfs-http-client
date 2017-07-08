package io.ipfs.api;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public class Multipart {
    private final String boundary;
    private static final String LINE_FEED = "\r\n";
    private HttpURLConnection httpConn;
    private String charset;
    private OutputStream out;
    private PrintWriter writer;

    public Multipart(String requestURL, String charset) {
        this.charset = charset;

        boundary = createBoundary();

        try {
            URL url = new URL(requestURL);
            httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setUseCaches(false);
            httpConn.setDoOutput(true);
            httpConn.setDoInput(true);
            httpConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            httpConn.setRequestProperty("User-Agent", "Java IPFS CLient");
            out = httpConn.getOutputStream();
            writer = new PrintWriter(new OutputStreamWriter(out, charset), true);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static String createBoundary() {
        Random r = new Random();
        String allowed = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder b = new StringBuilder();
        for (int i=0; i < 32; i++)
            b.append(allowed.charAt(r.nextInt(allowed.length())));
        return b.toString();
    }

    public void addFormField(String name, String value) {
        writer.append("--" + boundary).append(LINE_FEED);
        writer.append("Content-Disposition: form-data; name=\"" + name + "\"")
                .append(LINE_FEED);
        writer.append("Content-Type: text/plain; charset=" + charset).append(
                LINE_FEED);
        writer.append(LINE_FEED);
        writer.append(value).append(LINE_FEED);
        writer.flush();
    }

    public void addSubtree(Path parentPath, NamedStreamable dir) throws IOException {
        Path dirPath = parentPath.resolve(dir.getName().get());
        addDirectoryPart(dirPath);
        for (NamedStreamable f: dir.getChildren()) {
            if (f.isDirectory())
                addSubtree(dirPath, f);
            else
                addFilePart("file", f);
        }
    }

    public void addDirectoryPart(Path path) {
        writer.append("--" + boundary).append(LINE_FEED);
        writer.append("Content-Disposition: file; filename=\"" + encode(path.toString()) + "\"").append(LINE_FEED);
        writer.append("Content-Type: application/x-directory").append(LINE_FEED);
        writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.flush();
    }

    private static String encode(String in) {
        try {
            return URLEncoder.encode(in, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public void addFilePart(String fieldName, NamedStreamable uploadFile) {
        Optional<String> fileName = uploadFile.getName().map(n -> encode(n));
        writer.append("--" + boundary).append(LINE_FEED);
        if (!fileName.isPresent())
            writer.append("Content-Disposition: file; name=\"" + fieldName + "\";").append(LINE_FEED);
        else
            writer.append("Content-Disposition: file; filename=\"" + fileName.get() + "\";").append(LINE_FEED);
        writer.append("Content-Type: application/octet-stream").append(LINE_FEED);
        writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.flush();

        try {
            InputStream inputStream = uploadFile.getInputStream();
            byte[] buffer = new byte[4096];
            int r;
            while ((r = inputStream.read(buffer)) != -1)
                out.write(buffer, 0, r);
            out.flush();
            inputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        writer.append(LINE_FEED);
        writer.flush();
    }

    public void addHeaderField(String name, String value) {
        writer.append(name + ": " + value).append(LINE_FEED);
        writer.flush();
    }

    public String finish() {
        StringBuilder b = new StringBuilder();

        writer.append("--" + boundary + "--").append(LINE_FEED);
        writer.close();

        try {
            int status = httpConn.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        httpConn.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    b.append(line);
                }
                reader.close();
                httpConn.disconnect();
            } else {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(
                            httpConn.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        b.append(line);
                    }
                    reader.close();
                } catch (Throwable t) {
                }
                throw new IOException("Server returned status: " + status + " with body: " + b.toString() + " and Trailer header: " + httpConn.getHeaderFields().get("Trailer"));
            }

            return b.toString();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
