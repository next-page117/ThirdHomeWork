package com.example.thirdhomework;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import com.google.android.material.appbar.CollapsingToolbarLayout;

public class ShowActivity extends AppCompatActivity {
    private String appName;
    private int flag;

    private ImageView imageView;
    private CollapsingToolbarLayout collapsingToolbarLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show);

        Intent intent=getIntent();
        appName=intent.getStringExtra("appName");
        flag=intent.getIntExtra("flag",0);

        collapsingToolbarLayout=findViewById(R.id.collapsing_toolbar);
        imageView = findViewById(R.id.show_image_view);

        switch (flag){
            case 0:
                imageView.setImageResource(R.drawable.frequent_app);
                break;
            case 1:
                imageView.setImageResource(R.drawable.long_app);
                break;
        }


        collapsingToolbarLayout.setTitle(appName);


    }
}