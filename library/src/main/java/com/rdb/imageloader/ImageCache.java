package com.rdb.imageloader;

import android.graphics.Bitmap;
import android.util.LruCache;

public class ImageCache extends LruCache<String, Bitmap> {

    public ImageCache(int maxSize) {
        super(maxSize);
    }

    @Override
    protected int sizeOf(String key, Bitmap value) {
        return value.getByteCount();
    }

    @Override
    protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
        super.entryRemoved(evicted, key, oldValue, newValue);
    }
}
