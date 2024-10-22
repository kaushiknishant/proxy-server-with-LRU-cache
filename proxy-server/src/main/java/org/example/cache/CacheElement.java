package org.example.cache;

public class CacheElement {
    StringBuilder data;
    int len;
    StringBuilder url;
    long lruTimeTrack;
    CacheElement next;

    public CacheElement(StringBuilder data, int len, StringBuilder url, long lruTimeTrack) {
        this.data = data;
        this.len = len;
        this.url = url;
        this.lruTimeTrack = lruTimeTrack;
    }
}
