package com.example.helloworld;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class MainActivity2 extends AppCompatActivity {
    private TextView textView;
    private Button buttonColor;
    private Button buttonBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        textView = findViewById(R.id.textView);
        buttonColor = findViewById(R.id.buttonColor);
        buttonBack = findViewById(R.id.buttonBack);

        // 顯示第一頁傳來的文字（練習一的部分）
        Intent intent = getIntent();
        String inputText = intent.getStringExtra("InputText");
        textView.setText(inputText);

        // Change color：隨機改變文字顏色
        buttonColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Random random = new Random();
                int red = random.nextInt(256);
                int green = random.nextInt(256);
                int blue = random.nextInt(256);

                int color = Color.rgb(red, green, blue);
                textView.setTextColor(color);
            }
        });

        // Back：回上一頁
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
}