package com.rdb.imageloader;

/**
 * Created by DB on 2017/7/11.
 */

public class ImageLoadConfig {

    private final String diskCachePath;
    private final long diskCacheInvalidTime;

    public ImageLoadConfig(Builder builder) {
        this.diskCachePath = builder.diskCachePath;
        this.diskCacheInvalidTime = builder.diskCacheInvalidTime;
    }

    public String getDiskCachePath() {
        return diskCachePath;
    }

    public long getDiskCacheInvalidTime() {
        return diskCacheInvalidTime;
    }

    public static class Builder {

        private String diskCachePath;
        private long diskCacheInvalidTime;

        public Builder setDiskCachePath(String diskCachePath) {
            this.diskCachePath = diskCachePath;
            return this;
        }

        public Builder setDiskCacheInvalidTime(long diskCacheInvalidTime) {
            this.diskCacheInvalidTime = diskCacheInvalidTime;
            return this;
        }

        public ImageLoadConfig build() {
            return new ImageLoadConfig(this);
        }
    }
}
