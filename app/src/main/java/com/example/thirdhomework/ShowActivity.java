package com.example.thirdhomework;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.thirdhomework.entity.UsageData;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import java.util.List;

public class ShowActivity extends AppCompatActivity {
    private ImageView imageView;

    private String appName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show);

        Intent intent=getIntent();
        appName=intent.getStringExtra("appName");

        CollapsingToolbarLayout collapsingToolbarLayout=findViewById(R.id.collapsing_toolbar);
        collapsingToolbarLayout.setTitle(appName);

        imageView = findViewById(R.id.show_image_view);
        imageView.setImageResource(R.drawable.pic);


    }
}