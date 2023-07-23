package com.rdb.imageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.widget.ImageView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by DB on 2017/7/10.
 */
class ImageHandler extends Handler {

    private ImageCache imageCache;
    private SparseArray<String> viewArray;
    private List<ImageDecoder> imageDecoders = new ArrayList<>();
    private Map<Object, Future> tagMap = new WeakHashMap<>();
    private ExecutorService httpThreadExecutor;
    private ExecutorService localThreadExecutor;

    public ImageHandler(ImageCache imageCache, SparseArray<String> viewArray) {
        super(Looper.getMainLooper());
        this.imageCache = imageCache;
        this.viewArray = viewArray;
        httpThreadExecutor = Executors.newFixedThreadPool(3, new ThreadFactory() {
            private final AtomicInteger mCount = new AtomicInteger(1);

            public Thread newThread(Runnable r) {
                return new Thread(r, "HttpThread #" + mCount.getAndIncrement());
            }
        });
        localThreadExecutor = Executors.newFixedThreadPool(3, new ThreadFactory() {
            private final AtomicInteger mCount = new AtomicInteger(1);

            public Thread newThread(Runnable r) {
                return new Thread(r, "LocalThread #" + mCount.getAndIncrement());
            }
        });
    }

    public void load(Context context, ImageView imageView, String imageUri, ImageLoadListener loadListener, ImageLoadOptions imageLoadOptions) {
        if (context != null) {
            if (imageUri == null || TextUtils.isEmpty(imageUri)) {
                if (imageView != null && imageLoadOptions != null && imageLoadOptions.shouldShowImageForEmpty()) {
                    imageView.setImageDrawable(imageLoadOptions.getImageForEmpty(context.getResources()));
                }
            } else {
                String imageTag = imageUri;
                Bitmap bitmap;
                String cacheKey = imageTag;
                if (imageLoadOptions == null) {
                    imageLoadOptions = ImageLoader.getDefaultOptions();
                }
                WeakReference<ImageView> imageViewReference = null;
                if (imageView == null) {
                    bitmap = imageCache.get(imageTag);
                } else {
                    if (imageTag.equals(viewArray.get(imageView.hashCode()))) {
                        if (imageLoadOptions.shouldShowImageOnLoading()) {
                            imageView.setImageDrawable(imageLoadOptions.getImageOnLoading(context.getResources()));
                        }
                        return;
                    }
                    viewArray.put(imageView.hashCode(), imageTag);
                    imageViewReference = new WeakReference<>(imageView);
                    bitmap = imageCache.get(imageTag);
                    if (bitmap == null && imageView.getWidth() > 0) {
                        cacheKey = imageTag + "_" + imageView.getWidth();
                        bitmap = imageCache.get(cacheKey);
                    }
                }
                if (bitmap != null) {
                    handSuccess(imageViewReference, loadListener, bitmap, imageUri, cacheKey, imageTag, imageLoadOptions);
                } else {
                    if (imageView != null) {
                        Future future = tagMap.remove(imageView);
                        if (future != null) {
                            future.cancel(false);
                        }
                    }
                    if (imageView != null && imageLoadOptions.shouldShowImageOnLoading()) {
                        imageView.setImageDrawable(imageLoadOptions.getImageOnLoading(context.getResources()));
                    }
                    executeDecodeRunnable(context, imageViewReference, imageUri, loadListener, imageLoadOptions);
                }
                logCache();
            }
        }
    }

    private void executeDecodeRunnable(Context context, WeakReference<ImageView> imageViewReference, String imageUri, ImageLoadListener loadListener, ImageLoadOptions imageLoadOptions) {
        ImageDecoder imageDecoder;
        ImageDecoder.Scheme scheme;
        if (imageDecoders.size() > 0) {
            imageDecoder = imageDecoders.remove(0);
            scheme = imageDecoder.update(context, imageViewReference, imageUri, loadListener, imageLoadOptions);
        } else {
            imageDecoder = new ImageDecoder(this);
            scheme = imageDecoder.update(context, imageViewReference, imageUri, loadListener, imageLoadOptions);
        }
        Future future;
        if (scheme == ImageDecoder.Scheme.HTTP || scheme == ImageDecoder.Scheme.HTTPS) {
            future = httpThreadExecutor.submit(imageDecoder);
        } else {
            future = localThreadExecutor.submit(imageDecoder);
        }
        if (imageViewReference != null) {
            ImageView imageView = imageViewReference.get();
            if (imageView != null) {
                tagMap.put(imageView, future);
            }
        }
    }

    private void logCache() {
        Log.e(ImageLoader.LOG_TAG, "ImageHandler imageCache  " + imageCache.size());
    }

    void addImageDecoder(ImageDecoder imageDecoder) {
        imageDecoders.add(imageDecoder);
    }

    String getViewTag(ImageView imageView) {
        return viewArray.get(imageView.hashCode());
    }

    void handStart(final ImageLoadListener loadListener, final String imageUri) {
        if (loadListener != null) {
            post(new Runnable() {
                @Override
                public void run() {
                    loadListener.onLoadStart(imageUri);
                }
            });
        }
    }

    void handCancel(final WeakReference<ImageView> imageViewReference, final ImageLoadListener loadListener, final String imageUri, final String viewValue) {
        post(new Runnable() {
            @Override
            public void run() {
                if (imageViewReference != null) {
                    ImageView imageView = imageViewReference.get();
                    if (imageView != null && viewValue.equals(viewArray.get(imageView.hashCode()))) {
                        viewArray.remove(imageView.hashCode());
                    }
                }
                if (loadListener != null) {
                    loadListener.onLoadCancel(imageUri);
                }
            }
        });
    }

    void handSuccess(final WeakReference<ImageView> imageViewReference,
                     final ImageLoadListener loadListener, final Bitmap bitmap,
                     final String imageUri, final String cacheKey, final String viewValue, final ImageLoadOptions options) {
        if (options.isCacheInMemory()) {
            imageCache.put(cacheKey, bitmap);
        }
        Log.e(ImageLoader.LOG_TAG, "ImageHandler handSuccess " + viewValue);
        post(new Runnable() {
            @Override
            public void run() {
                if (imageViewReference != null && imageUri != null) {
                    ImageView imageView = imageViewReference.get();
                    if (imageView != null
                            && viewValue.equals(viewArray.get(imageView.hashCode()))) {
                        Log.e(ImageLoader.LOG_TAG, "ImageHandler setImageBitmap " + viewValue);
                        imageView.setImageBitmap(bitmap);
                        viewArray.remove(imageView.hashCode());
                    }
                }
                if (loadListener != null) {
                    loadListener.onLoadSuccess(bitmap, imageUri);
                }
            }
        });
    }

    void handError(final Context context, final WeakReference<ImageView> imageViewReference, final ImageLoadListener loadListener,
                   final Exception e, final String imageUri, final String viewValue, final ImageLoadOptions options) {
        post(new Runnable() {
            @Override
            public void run() {
                if (imageViewReference != null && imageUri != null && options.shouldShowImageOnFail()) {
                    ImageView imageView = imageViewReference.get();
                    if (imageView != null
                            && viewValue.equals(viewArray.get(imageView.hashCode()))) {
                        imageView.setImageDrawable(options.getImageOnFail(context.getResources()));
                        viewArray.remove(imageView.hashCode());
                    }
                }
                if (loadListener != null) {
                    loadListener.onLoadError(e, imageUri);
                }
            }
        });
    }
}
