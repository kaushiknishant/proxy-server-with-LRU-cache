package org.example.cache;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class LRUCache extends LinkedHashMap<String, CacheElement> {

    public static final int MAX_BYTES = 4096; // Max allowed size of request/response
    public static final int MAX_CLIENTS = 400; // Max number of client requests served at a time
    public static final int MAX_SIZE = 200 * (1 << 20); // Size of the cache (200 MB)
    public static final int MAX_ELEMENT_SIZE = 10 * (1 << 20); // Max size of an element in cache (10 MB)

    private final int capacity;
    private int currentSize;

    public LRUCache(int capacity) {
        // Initialize with capacity + 1 size, load factor 0.75, and access-order=true for LRU behavior
        super(capacity + 1, 0.75f, true);
        this.capacity = capacity;
        this.currentSize = 0;
    }

    public CacheElement find(String url) {
        CacheElement element = super.get(url);
        if (element != null) {
            element.setLastAccessTime(Instant.now());
            // Move to front due to access (handled automatically by LinkedHashMap)
            super.get(url);
        }
        return element;
    }

    public void removeLeastRecentlyUsed() {
        if (!isEmpty()) {
            Map.Entry<String, CacheElement> lru = this.entrySet().iterator().next();
            CacheElement element = lru.getValue();
            currentSize -= element.getSize();
            this.remove(lru.getKey());
        }
    }

    public boolean addCacheElement(byte[] data, int size, String url, Instant lruTimeTrack) {
        // Check if the element size is within limits
        if (size > MAX_ELEMENT_SIZE) {
            return false;
        }

        // Remove elements until we have space for the new element
        while (currentSize + size > MAX_SIZE && !isEmpty()) {
            removeLeastRecentlyUsed();
        }

        // If still too big after removing elements, return false
        if (currentSize + size > MAX_SIZE) {
            return false;
        }

        CacheElement newElement = new CacheElement(data, size, lruTimeTrack);
        super.put(url, newElement);
        currentSize += size;
        return true;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<String, CacheElement> eldest) {
        // This method is called by LinkedHashMap after each put/putAll operation
        return size() > capacity;
    }

    public int getCurrentSize() {
        return currentSize;
    }
}
