package com.bakban9.storage;

public interface StorageService {

    /** Stores the bytes under the key and returns the public URL. */
    String put(String key, byte[] data, String contentType);

    void delete(String key);
}
