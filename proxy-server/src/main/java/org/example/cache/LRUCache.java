package org.example.cache;

import java.time.Instant;

public class LRUCache {

    public static final int MAX_BYTES = 4096; // Max allowed size of request/response
    public static final int MAX_CLIENTS = 400; // Max number of client requests served at a time
    public static final int MAX_SIZE = 200 * (1 << 20); // Size of the cache (200 MB)
    public static final int MAX_ELEMENT_SIZE = 10 * (1 << 20); // Max size of an element in cache (10 MB)

    public CacheElement find(StringBuilder url){
        return null;
    }
    public void removeCacheElement(){

    }
    public boolean addCacheElement(byte[] data, int size, String url, Instant lruTimeTrack){
        return false;
    }
}
