package com.rdb.imageloader;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;

/**
 * Created by DB on 2017/7/10.
 */

public final class ImageLoadOptions {

    private final int imageResOnLoading;
    private final int imageResForEmpty;
    private final int imageResOnFail;
    private final Drawable imageOnLoading;
    private final Drawable imageForEmpty;
    private final Drawable imageOnFail;
    private final boolean cacheInMemory;
    private final boolean cacheOnDisk;
    private final BitmapFactory.Options decodingOptions;

    private ImageLoadOptions(Builder builder) {
        imageResOnLoading = builder.imageResOnLoading;
        imageResForEmpty = builder.imageResForEmpty;
        imageResOnFail = builder.imageResOnFail;
        imageOnLoading = builder.imageOnLoading;
        imageForEmpty = builder.imageForEmpty;
        imageOnFail = builder.imageOnFail;
        cacheInMemory = builder.cacheInMemory;
        cacheOnDisk = builder.cacheOnDisk;
        decodingOptions = builder.decodingOptions == null ? new BitmapFactory.Options() : builder.decodingOptions;
        if (decodingOptions.inPreferredConfig == null) {
            decodingOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        }
    }

    public boolean shouldShowImageOnLoading() {
        return imageOnLoading != null || imageResOnLoading != 0;
    }

    public boolean shouldShowImageForEmpty() {
        return imageForEmpty != null || imageResForEmpty != 0;
    }

    public boolean shouldShowImageOnFail() {
        return imageOnFail != null || imageResOnFail != 0;
    }

    public Drawable getImageOnLoading(Resources res) {
        return imageResOnLoading != 0 ? res.getDrawable(imageResOnLoading) : imageOnLoading;
    }

    public Drawable getImageForEmpty(Resources res) {
        return imageResForEmpty != 0 ? res.getDrawable(imageResForEmpty) : imageForEmpty;
    }

    public Drawable getImageOnFail(Resources res) {
        return imageResOnFail != 0 ? res.getDrawable(imageResOnFail) : imageOnFail;
    }

    public boolean isCacheInMemory() {
        return cacheInMemory;
    }

    public boolean isCacheOnDisk() {
        return cacheOnDisk;
    }

    public BitmapFactory.Options getDecodingOptions() {
        return decodingOptions;
    }

    public static class Builder {
        private int imageResOnLoading;
        private int imageResForEmpty;
        private int imageResOnFail;
        private Drawable imageOnLoading;
        private Drawable imageForEmpty;
        private Drawable imageOnFail;
        private boolean cacheInMemory;
        private boolean cacheOnDisk;
        private BitmapFactory.Options decodingOptions;


        public Builder showImageOnLoading(int imageRes) {
            imageResOnLoading = imageRes;
            return this;
        }

        public Builder showImageOnLoading(Drawable drawable) {
            imageOnLoading = drawable;
            return this;
        }

        public Builder showImageForEmpty(int imageRes) {
            imageResForEmpty = imageRes;
            return this;
        }

        public Builder showImageForEmpty(Drawable drawable) {
            imageForEmpty = drawable;
            return this;
        }

        public Builder showImageOnFail(int imageRes) {
            imageResOnFail = imageRes;
            return this;
        }

        public Builder showImageOnFail(Drawable drawable) {
            imageOnFail = drawable;
            return this;
        }

        public Builder cacheInMemory(boolean cacheInMemory) {
            this.cacheInMemory = cacheInMemory;
            return this;
        }

        public Builder cacheOnDisk(boolean cacheOnDisk) {
            this.cacheOnDisk = cacheOnDisk;
            return this;
        }

        public Builder decodingOptions(BitmapFactory.Options decodingOptions) {
            if (decodingOptions == null)
                throw new IllegalArgumentException("decodingOptions can't be null");
            this.decodingOptions = decodingOptions;
            return this;
        }

        public ImageLoadOptions build() {
            return new ImageLoadOptions(this);
        }
    }
}
