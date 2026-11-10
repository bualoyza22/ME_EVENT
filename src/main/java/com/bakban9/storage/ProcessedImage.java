package com.bakban9.storage;

import java.util.Map;

/** Result of processing an upload: the file to store, its size and resized variants (width → bytes). */
public record ProcessedImage(ImageType type, byte[] original, Integer width, Integer height,
                             String variantExt, Map<Integer, byte[]> variants) {}
