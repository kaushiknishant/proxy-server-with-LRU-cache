package org.example.cache;

import java.time.Instant;

public class CacheElement {
    private final byte[] data;
    private final int size;
    private Instant lastAccessTime;

    public CacheElement(byte[] data, int size, Instant lastAccessTime) {
        this.data = data;
        this.size = size;
        this.lastAccessTime = lastAccessTime;
    }

    public byte[] getData() {
        return data;
    }

    public int getSize() {
        return size;
    }

    public void setLastAccessTime(Instant lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public Instant getLastAccessTime() {
        return lastAccessTime;
    }
}
