package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {
    private TextView tvTitle;
    private TextView tvMessages;
    private EditText etMessage;
    private Button btnSend;
    private Button btnLeave;

    private Socket socket;
    private BufferedReader reader;
    private OutputStreamWriter outputSW;
    private MsgObj msg;
    private String tmp;

    private String myName;      // ← 從紙條拿到的
    private String serverIP;    // ← 從紙條拿到的
    private int serverPort;     // ← 從紙條拿到的
    private volatile boolean leaving = false;   // 是不是我自己按了離開

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvTitle = findViewById(R.id.tvTitle);
        tvMessages = findViewById(R.id.tvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnLeave = findViewById(R.id.btnLeave);

        // 打開第一頁遞來的紙條，用「標籤」找出三個值
        myName = getIntent().getStringExtra("name");
        serverIP = getIntent().getStringExtra("ip");
        serverPort = Integer.parseInt(getIntent().getStringExtra("port"));

        tvTitle.setText("我是：" + myName);
        tvMessages.setText("Connecting to " + serverIP + ":" + serverPort + "...\n");

        // 一進到這頁就自動去連線（不用再按 Connect）
        new Thread(new clientThread()).start();

        // 按 Send：開新 thread 送訊息
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String text = etMessage.getText().toString();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sendMessage(text);
                    }
                }).start();
                etMessage.setText("");
            }
        });

        // 按離開：關掉這一頁（onDestroy 會負責掛斷電話）
        btnLeave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    class clientThread implements Runnable {
        @Override
        public void run() {
            try {
                socket = new Socket(serverIP, serverPort);   // 主動去敲 Server 的門
                msg = new MsgObj(myName);                    // 打包機用我的名字
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvMessages.append("Connected\n");
                    }
                });

                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                outputSW = new OutputStreamWriter(socket.getOutputStream());
                outputSW.write(msg.toJson("join", "") + "\n");   // 跟大家說：我來了
                outputSW.flush();

                while ((tmp = reader.readLine()) != null) {
                    JSONObject jsonObj = new JSONObject(tmp);
                    runOnUiThread(new Runnable() {
                        JSONObject _jsonObj;

                        @Override
                        public void run() {
                            try {
                                String type = _jsonObj.getString(MsgObj.LABEL_TYPE);
                                String name = _jsonObj.getString(MsgObj.LABEL_NAME);

                                if (type.equals("reject")) {
                                    // 名字重複被 Server 拒絕：跳出提示，回到第一頁重新取名
                                    String reason = _jsonObj.getString(MsgObj.LABEL_MSG);
                                    Toast.makeText(MainActivity.this, reason, Toast.LENGTH_LONG).show();
                                    finish();
                                } else if (type.equals("join")) {
                                    tvMessages.append(name + " 進來了\n");
                                } else if (type.equals("leave")) {
                                    tvMessages.append(name + " 離開了\n");
                                } else {
                                    String msg = _jsonObj.getString(MsgObj.LABEL_MSG);
                                    if (name.equals(myName)) {
                                        tvMessages.append("Me: " + msg + "\n");   // 自己說的話
                                    } else {
                                        tvMessages.append(name + ": " + msg + "\n");
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        public Runnable init(JSONObject jsonObj) {
                            _jsonObj = jsonObj;
                            return this;
                        }
                    }.init(jsonObj));
                }

                // 跳出 while 代表 Server 斷線了
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvMessages.append("Server disconnected\n");
                    }
                });
            } catch (IOException | JSONException e) {
                if (leaving) return;   // 是我自己按離開造成的斷線，不算錯誤
                e.printStackTrace();
                final String error = e.toString();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvMessages.append("Connection failed: " + error + "\n");
                    }
                });
            }
        }
    }

    // 送訊息給 Server
    private void sendMessage(final String text) {
        if (outputSW == null) return;   // 還沒連上，不能送
        try {
            outputSW.write(msg.toJson("msg", text) + "\n");
            outputSW.flush();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    // 這一頁被關掉時（按離開、或按手機的返回鍵）都會執行這裡：掛斷電話
    @Override
    protected void onDestroy() {
        super.onDestroy();
        leaving = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (socket != null) socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}