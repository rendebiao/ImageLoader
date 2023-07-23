package com.rdb.imageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by DB on 2017/7/10.
 */
class ImageDecoder implements Runnable {

    private Scheme scheme;
    private Context context;
    private String imageUri;
    private ImageHandler imageHandler;
    private ImageLoadOptions loadOptions;
    private ImageLoadListener loadListener;
    private WeakReference<ImageView> imageViewReference;

    public ImageDecoder(ImageHandler imageHandler) {
        this.imageHandler = imageHandler;
    }

    public Scheme update(Context context, WeakReference<ImageView> imageViewReference, String imageUri, ImageLoadListener loadListener, ImageLoadOptions loadOptions) {
        this.context = context;
        this.imageUri = imageUri;
        this.loadOptions = loadOptions;
        this.loadListener = loadListener;
        this.imageViewReference = imageViewReference;
        scheme = Scheme.ofUri(imageUri);
        return scheme;
    }

    @Override
    public void run() {
        Log.e(ImageLoader.LOG_TAG, "ImageDecoder " + Thread.currentThread().getName() + " start " + imageUri);
        long startTime = System.currentTimeMillis();
        imageHandler.handStart(loadListener, imageUri);
        String tag = imageUri;
        ImageView view = null;
        if (imageViewReference != null) {
            view = imageViewReference.get();
        }
        if (view != null && !tag.equals(imageHandler.getViewTag(view))) {
            imageHandler.handCancel(imageViewReference, loadListener, imageUri, tag);
        } else {
            BitmapResult result = getBitmap(view);
            if (result.bitmap == null) {
                imageHandler.handError(context, imageViewReference, loadListener, result.exception, imageUri, tag, loadOptions);
            } else {
                imageHandler.handSuccess(imageViewReference, loadListener, result.bitmap, imageUri, result.cacheSize <= 0 ? tag : (tag + "_" + result.cacheSize), tag, loadOptions);
            }
        }
        imageHandler.addImageDecoder(this);
        Log.e(ImageLoader.LOG_TAG, "ImageDecoder " + Thread.currentThread().getName() + " finish " + (System.currentTimeMillis() - startTime) + " " + imageUri);
    }

    private BitmapResult getBitmap(ImageView imageView) {
        if (scheme == Scheme.HTTP || scheme == Scheme.HTTPS) {
            return getBitmapFromNetwork(imageView, imageUri);
        } else if (scheme == Scheme.CONTENT || scheme == Scheme.FILE || scheme == Scheme.ASSETS || scheme == Scheme.DRAWABLE) {
            return getBitmapFromUri(imageView, imageUri, scheme);
        } else {
            return getBitmapFromFilePath(imageView, imageUri);
        }
    }

    private BitmapResult getBitmapFromUri(ImageView imageView, String imageUri, Scheme scheme) {
        BitmapResult result = new BitmapResult();
        Bitmap old = null;
        InputStream is = null;
        Uri uri = Uri.parse(imageUri);
        try {
            if (scheme == Scheme.CONTENT || scheme == Scheme.FILE) {
                is = context.getContentResolver().openInputStream(uri);
            } else if (scheme == Scheme.ASSETS) {
                is = context.getAssets().open(Scheme.ASSETS.crop(imageUri));
            } else if (scheme == Scheme.DRAWABLE) {
                is = context.getResources().openRawResource(Integer.parseInt(Scheme.DRAWABLE.crop(imageUri)));
            }
            if (is != null) {
                old = BitmapFactory.decodeStream(is, null, loadOptions.getDecodingOptions());
            }
            if (old != null) {
                result.bitmap = scaleBitmap(imageView, old);
                if (imageView != null) {
                    result.cacheSize = imageView.getWidth();
                }
            }
        } catch (Exception e) {
            Log.e(ImageLoader.LOG_TAG, "ImageDecoder Unable to open imageUri: " + imageUri, e);
            result.exception = e;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.e(ImageLoader.LOG_TAG, "ImageDecoder Unable to close InputStream: " + imageUri, e);
                }
            }
        }
        return result;
    }

    private BitmapResult getBitmapFromFilePath(ImageView imageView, String imageUri) {
        BitmapResult result = new BitmapResult();
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imageUri, options);
            if (imageView != null) {
                int count = 0;
                while (count < 5 && imageView.getWidth() == 0 && imageView.getHeight() == 0) {
                    Thread.sleep(50);
                    count++;
                }
                options.inSampleSize = getInSampleSize(imageView, options.outWidth, options.outHeight);
                result.cacheSize = imageView.getWidth();
            }
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = loadOptions.getDecodingOptions().inPreferredConfig;
            result.bitmap = BitmapFactory.decodeFile(imageUri, options);
        } catch (Exception e) {
            Log.e(ImageLoader.LOG_TAG, "ImageDecoder Unable to open content: " + imageUri, e);
            result.exception = e;
        }
        return result;
    }

    private BitmapResult getBitmapFromNetwork(ImageView imageView, String imageUri) {
        BitmapResult result = new BitmapResult();
        Bitmap old = null;
        InputStream is = null;
        try {
            File cacheFile = getCacheFile(imageUri);
            if (cacheFile != null && cacheFile.exists()) {
                old = BitmapFactory.decodeFile(cacheFile.getAbsolutePath(), loadOptions.getDecodingOptions());
                Log.e(ImageLoader.LOG_TAG, "ImageDecoder loadFromDiskCache: " + imageUri);
            }
            if (old == null) {
                URL url = new URL(imageUri);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                Matcher m = Pattern.compile("^http://[^/]+").matcher(imageUri);
                while (m.find()) {
                    conn.setRequestProperty("referer", m.group());
                }
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(20000);
                if (conn.getResponseCode() == 200) {
                    is = conn.getInputStream();
                    Log.e(ImageLoader.LOG_TAG, "ImageDecoder loadFromNetwork: " + imageUri);
                }
                boolean useInputStream = false;
                if (is != null && loadOptions.isCacheOnDisk() && cacheFile != null) {
                    useInputStream = cacheImageToDisk(is, cacheFile);
                    Log.e(ImageLoader.LOG_TAG, "ImageDecoder cacheImageToDisk: " + imageUri);
                }
                if (!useInputStream) {
                    old = BitmapFactory.decodeStream(is, null, loadOptions.getDecodingOptions());
                } else if (cacheFile.exists()) {
                    old = BitmapFactory.decodeFile(cacheFile.getAbsolutePath(), loadOptions.getDecodingOptions());
                }
            }
            result.bitmap = scaleBitmap(imageView, old);
            if (imageView != null) {
                result.cacheSize = imageView.getWidth();
            }
        } catch (Exception e) {
            result.exception = e;
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    private File getCacheFile(String imageTag) {
        String path = ImageLoader.getImageLoadConfig().getDiskCachePath();
        if (!TextUtils.isEmpty(path)) {
            File directory = new File(path);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            if (directory.exists()) {
                File file = new File(directory.getAbsolutePath() + "/" + String.valueOf(imageTag.hashCode()));
                if (file.lastModified() + ImageLoader.getImageLoadConfig().getDiskCacheInvalidTime() < System.currentTimeMillis()) {
                    file.delete();
                }
                return file;
            }
        }
        return null;
    }

    private int getInSampleSize(ImageView imageView, int width, int height) {
        int inSampleSize;
        if (imageView.getWidth() > 0 && imageView.getHeight() > 0) {
            if (imageView.getHeight() * 1f / imageView.getWidth() > height
                    * 1f / width) {
                inSampleSize = width / imageView.getWidth();
            } else {
                inSampleSize = height / imageView.getHeight();
            }
        } else {
            inSampleSize = width / 1000;
        }
        return inSampleSize;
    }

    private Bitmap scaleBitmap(ImageView imageView, Bitmap old) {
        Bitmap bitmap = old;
        if (imageView != null && imageView.getWidth() > 0 && imageView.getHeight() > 0) {
            if (imageView.getHeight() * 1f / imageView.getWidth() > old.getHeight() * 1f / old.getWidth()) {
                bitmap = Bitmap.createScaledBitmap(old, imageView.getWidth(),
                        old.getHeight() * imageView.getWidth() / old.getWidth(), false);
            } else {
                bitmap = Bitmap.createScaledBitmap(old, old.getWidth() * imageView.getHeight() / old.getHeight(),
                        imageView.getHeight(), false);
            }
            old.recycle();
        }
        return bitmap;
    }


    private boolean cacheImageToDisk(InputStream inputStream, File file) {
        boolean useInputStream = false;
        FileOutputStream outStream = null;
        try {
            if (file.exists()) {
                file.delete();
            }
            File tempFile = new File(file.getAbsolutePath() + "_temp");
            if (tempFile.exists()) {
                tempFile.delete();
            }
            tempFile.createNewFile();
            byte[] buffer = new byte[4096];
            int len;
            outStream = new FileOutputStream(tempFile);
            useInputStream = true;
            while ((len = inputStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, len);
            }
            tempFile.renameTo(file);
        } catch (Exception e) {
            file.delete();
            e.printStackTrace();
        } finally {
            if (outStream != null) {
                try {
                    outStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (useInputStream && inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return useInputStream;
    }

    enum Scheme {
        HTTP("http"), HTTPS("https"), FILE("file"), CONTENT("content"), ASSETS("assets"), DRAWABLE("drawable"), UNKNOWN("");
        private String scheme;
        private String uriPrefix;

        Scheme(String scheme) {
            this.scheme = scheme;
            uriPrefix = scheme + "://";
        }

        public static Scheme ofUri(String uri) {
            if (uri != null) {
                for (Scheme s : values()) {
                    if (s.belongsTo(uri)) {
                        return s;
                    }
                }
            }
            return UNKNOWN;
        }

        private boolean belongsTo(String uri) {
            return uri.toLowerCase(Locale.US).startsWith(uriPrefix);
        }

        public String wrap(String path) {
            return uriPrefix + path;
        }

        public String crop(String uri) {
            if (!belongsTo(uri)) {
                throw new IllegalArgumentException(String.format("URI [%1$s] doesn't have expected scheme [%2$s]", uri, scheme));
            }
            return uri.substring(uriPrefix.length());
        }
    }

    class BitmapResult {
        Bitmap bitmap;
        int cacheSize;
        Exception exception;
    }
}
