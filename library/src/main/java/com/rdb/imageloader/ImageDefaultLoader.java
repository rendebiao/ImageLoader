package com.rdb.imageloader;

import android.content.Context;
import android.util.SparseArray;
import android.widget.ImageView;

public class ImageDefaultLoader extends ImageLoader {

    private ImageHandler imageHandler;

    private ImageDefaultLoader(int cacheSize) {
        ImageCache imageCache = new ImageCache(cacheSize);
        SparseArray<String> viewArray = new SparseArray<>();
        imageHandler = new ImageHandler(imageCache, viewArray);
    }

    public synchronized static ImageDefaultLoader newInstance(int cacheSize) {
        return new ImageDefaultLoader(cacheSize);
    }

    @Override
    public void loadImage(Context context, ImageView imageView, String imageUrl) {
        loadImage(context, imageView, imageUrl, null, null);
    }

    @Override
    public void loadImage(Context context, ImageView imageView, String imageUrl, ImageLoadOptions imageLoadOptions) {
        loadImage(context, imageView, imageUrl, null, imageLoadOptions);
    }

    @Override
    public void loadImage(Context context, String imageUrl, ImageLoadListener loadListener) {
        loadImage(context, null, imageUrl, loadListener, null);
    }

    @Override
    public void loadImage(Context context, String imageUrl, ImageLoadListener loadListener, ImageLoadOptions imageLoadOptions) {
        loadImage(context, null, imageUrl, loadListener, imageLoadOptions);
    }

    @Override
    public void loadImage(Context context, ImageView imageView, String imageUrl, ImageLoadListener loadListener) {
        loadImage(context, imageView, imageUrl, loadListener, null);
    }

    @Override
    public void loadImage(Context context, ImageView imageView, String imageUrl, ImageLoadListener loadListener, ImageLoadOptions imageLoadOptions) {
        imageHandler.load(context, imageView, imageUrl, loadListener, imageLoadOptions);
    }
}
