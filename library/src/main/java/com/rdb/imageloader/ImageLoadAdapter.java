package com.rdb.imageloader;

import android.graphics.Bitmap;

/**
 * Created by DB on 2017/7/10.
 */
public abstract class ImageLoadAdapter implements ImageLoadListener {

    @Override
    public void onLoadStart(String imageUri) {

    }

    @Override
    public void onLoadCancel(String imageUri) {

    }

    @Override
    public void onLoadSuccess(Bitmap bitmap, String imageUri) {

    }

    @Override
    public void onLoadError(Exception e, String imageUri) {

    }
}
