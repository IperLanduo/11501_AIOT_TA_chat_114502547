package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText etName = findViewById(R.id.etName);
        EditText etIP = findViewById(R.id.etIP);
        EditText etPort = findViewById(R.id.etPort);
        Button btnJoin = findViewById(R.id.btnJoin);

        btnJoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 準備一張紙條：要從這頁 (LoginActivity) 跳到聊天頁 (MainActivity)
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                // 在紙條上寫三件事，每件事都有一個「標籤」方便對方找
                intent.putExtra("name", etName.getText().toString().trim());
                intent.putExtra("ip", etIP.getText().toString().trim());
                intent.putExtra("port", etPort.getText().toString().trim());
                startActivity(intent);   // 跳頁，紙條一起帶過去
            }
        });
    }
}