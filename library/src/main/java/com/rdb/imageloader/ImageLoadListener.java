package com.rdb.imageloader;

import android.graphics.Bitmap;

/**
 * Created by DB on 2017/7/10.
 */
public interface ImageLoadListener {

    void onLoadStart(String imageUri);

    void onLoadCancel(String imageUri);

    void onLoadSuccess(Bitmap bitmap, String imageUri);

    void onLoadError(Exception e, String imageUri);
}
