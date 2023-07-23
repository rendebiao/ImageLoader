package com.rdb.imageloader.demo;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.rdb.imageloader.ImageDefaultLoader;
import com.rdb.imageloader.ImageLoadConfig;
import com.rdb.imageloader.ImageLoadListener;
import com.rdb.imageloader.ImageLoader;

public class MainActivity extends AppCompatActivity {

    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.imageView);
        int memClass = ((ActivityManager) getSystemService(
                Context.ACTIVITY_SERVICE)).getMemoryClass();
        int cacheSize = 1024 * 1024 * memClass / 3;
        ImageLoader.init(ImageDefaultLoader.newInstance(cacheSize), new ImageLoadConfig.Builder()
                .build());
        ImageLoader.getInstance().loadImage(this, imageView,
                "https://lmg.jj20.com/up/allimg/tp08/51042223242816-lp.jpg", new ImageLoadListener() {
                    @Override
                    public void onLoadStart(String imageUri) {

                    }

                    @Override
                    public void onLoadCancel(String imageUri) {

                    }

                    @Override
                    public void onLoadSuccess(Bitmap bitmap, String imageUri) {
                        imageView.setImageBitmap(bitmap);
                    }

                    @Override
                    public void onLoadError(Exception e, String imageUri) {

                    }
                });
    }

}
