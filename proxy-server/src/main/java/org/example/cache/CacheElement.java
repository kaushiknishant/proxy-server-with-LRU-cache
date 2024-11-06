package org.example.cache;

import java.time.Instant;

public class CacheElement {
    private byte[] data;
    private int len;
    private String url;
    private Instant lruTimeTrack;;
    public CacheElement next;

    public CacheElement(byte[] data, int len, String url, Instant lruTimeTrack) {
        this.data = data;
        this.len = len;
        this.url = url;
        this.lruTimeTrack = lruTimeTrack;
    }
}
