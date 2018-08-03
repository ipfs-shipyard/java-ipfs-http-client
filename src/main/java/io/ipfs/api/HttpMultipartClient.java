package io.ipfs.api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.FormBodyPartBuilder;
import org.apache.http.entity.mime.MIME;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

public class HttpMultipartClient {
    
    final HttpPost post;
    final MultipartEntityBuilder builder;

    public HttpMultipartClient(URL url) throws IOException {
        url = fixupURL(url, "hash", "sha2-256");
        url = fixupURL(url, "stream-channels", "true");
        post = new HttpPost(url.toString());
        post.addHeader("User-Agent", "Java IPFS CLient");
        post.addHeader("Connection", "close");
        builder = MultipartEntityBuilder.create();
    }
    
    public HttpMultipartClient addBodyPart(NamedStreamable file) throws IOException {
        buildMultipartEntity(Arrays.asList(file));
        return this;
    }

    public HttpMultipartClient addBodyParts(List<NamedStreamable> files) throws IOException {
        buildMultipartEntity(files);
        return this;
    }

    @SuppressWarnings("unchecked")
    public List<MerkleNode> execute() throws IOException {
        
        HttpClient client = HttpClientBuilder.create().build();
        post.setEntity(builder.build());
        HttpResponse res = client.execute(post);
        
        String json = EntityUtils.toString(res.getEntity(), "UTF-8");
        //System.out.println(json);
        
        List<MerkleNode> tree = JSONParser.parseStream(json).stream()
                .map(x -> MerkleNode.fromJSON((Map<String, Object>) x))
                .collect(Collectors.toList());
        
        return tree;
    }
    
    private void buildMultipartEntity(List<NamedStreamable> files) throws IOException {
        for (NamedStreamable file : files) {
            buildMultipartEntity(builder, new AtomicInteger(), Paths.get(""), file);
        }
    }

    private void buildMultipartEntity(MultipartEntityBuilder builder, AtomicInteger part, Path parent, NamedStreamable file) throws IOException {
        
        String fileName = file.getName().orElse("");
        Path fullPath = parent.resolve(fileName);
        
        String encFileName = encode(fullPath.toString());
        String partName = String.format("part%03d", part.incrementAndGet());
        
        if (file.isDirectory()) {
            
            FormBodyPart bodyPart = FormBodyPartBuilder.create()
                .setName(partName)
                .addField(MIME.CONTENT_DISPOSITION, "file; filename=\"" + encFileName + "\"")
                .setBody(new StringBody("", ContentType.create("application/x-directory")))
                .build();
            
            builder.addPart(bodyPart);
            
            for (NamedStreamable child : file.getChildren()) {
                buildMultipartEntity(builder, part, fullPath, child);
            }
            
        } else {
            
            FormBodyPart bodyPart = FormBodyPartBuilder.create()
                    .setName(partName)
                    .addField(MIME.CONTENT_DISPOSITION, "file; filename=\"" + encFileName + "\"")
                    .setBody(new InputStreamBody(file.getInputStream(), ContentType.APPLICATION_OCTET_STREAM))
                    .build();
                
            builder.addPart(bodyPart);
        }
    }

    private URL fixupURL(URL url, String pname, String pvalue) {
        String query = url.getQuery() != null ? url.getQuery() : "";
        if (!query.contains(pname)) {
            if (query.length() > 0) query += "&";
            query += pname + "=" + pvalue;
        }
        try {
            url = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getPath() + "?" + query);
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException(ex);
        }
        return url;
    }

    private static String encode(String in) {
        try {
            return URLEncoder.encode(in, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}