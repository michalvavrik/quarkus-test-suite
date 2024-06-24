package io.quarkus.ts.http.restclient.reactive;

import java.util.List;
import java.util.Map;

public class Item {
    public final String name;
    public final long size;
    public final String charset;
    public final String fileName;
    public final boolean isFileItem;
    public final Map<String, List<String>> headers;

    public Item(String name, long size, String charset, String fileName, boolean isFileItem,
            Map<String, List<String>> headers) {
        this.name = name;
        this.size = size;
        this.charset = charset;
        this.fileName = fileName;
        this.isFileItem = isFileItem;
        this.headers = headers;
    }
}
