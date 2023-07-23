package com.rdb.imageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.widget.ImageView;

public abstract class ImageLoader {

    public static final String LOG_TAG = "ImageLoader";
    private static ImageLoader instance;
    private static ImageLoadConfig imageLoadConfig;
    private static ImageLoadOptions defaultOptions;

    public static void init(ImageLoader imageLoader, ImageLoadConfig config) {
        init(imageLoader, config, null);
    }

    public static void init(ImageLoader imageLoader, ImageLoadConfig config, ImageLoadOptions options) {
        if (config == null) {
            throw new RuntimeException("ImageLoadConfig can't be null");
        }
        instance = imageLoader;
        imageLoadConfig = config;
        if (options != null) {
            defaultOptions = options;
        }
    }

    public static ImageLoadOptions getDefaultOptions() {
        if (defaultOptions == null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            defaultOptions = new ImageLoadOptions.Builder()
                    .cacheInMemory(true)
                    .cacheOnDisk(true)
                    .decodingOptions(options)
                    .showImageOnLoading(new ColorDrawable(0xfff0f0f0)).build();
        }
        return defaultOptions;
    }

    public static ImageLoadConfig getImageLoadConfig() {
        return imageLoadConfig;
    }

    public static ImageLoader getInstance() {
        return instance;
    }

    public abstract void loadImage(Context context, ImageView imageView, String imageUri);

    public abstract void loadImage(Context context, ImageView imageView, String imageUri, ImageLoadOptions imageLoadOptions);

    public abstract void loadImage(Context context, String imageUri, ImageLoadListener loadListener);

    public abstract void loadImage(Context context, String imageUri, ImageLoadListener loadListener, ImageLoadOptions imageLoadOptions);

    public abstract void loadImage(Context context, ImageView imageView, String imageUri, ImageLoadListener loadListener);

    public abstract void loadImage(Context context, ImageView imageView, String imageUri, ImageLoadListener loadListener, ImageLoadOptions imageLoadOptions);

}
