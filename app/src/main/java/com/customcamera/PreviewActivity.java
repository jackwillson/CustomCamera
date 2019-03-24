package com.customcamera;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.io.File;

public class PreviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        final ImageView imgPreview= (ImageView) findViewById(R.id.imgPreview);

        Glide.with(this).load(new File(getIntent().getStringExtra("pic"))).asBitmap().into(new SimpleTarget<Bitmap>(getDisplayWidth(), getDisplayHeight()){
            @Override
            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                imgPreview.setImageBitmap(resource);
            }
        });
    }
    public int getDisplayWidth() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x;
    }

    public int getDisplayHeight() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.y;
    }
}
