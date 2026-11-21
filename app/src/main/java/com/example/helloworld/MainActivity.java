package com.example.helloworld;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MainActivity extends AppCompatActivity {
    private static final int SERVER_PORT = 8888;

    private TextView tvIP;
    private TextView tvMessages;
    private EditText etMessage;
    private Button btnSend;
    private Button btnLeave;

    private ServerSocket serverSocket;
    private MsgObj msg;
    private String serverName;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();   // 點名簿

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvIP = findViewById(R.id.tvIP);
        tvMessages = findViewById(R.id.tvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnLeave = findViewById(R.id.btnLeave);

        // 讀第一頁傳來的 Server 名稱；還沒有第一頁時就先叫 "Server"
        serverName = getIntent().getStringExtra("name");
        if (serverName == null || serverName.isEmpty()) serverName = "Server";
        msg = new MsgObj(serverName);

        String myIp = getLocalIp();
        tvIP.setText(serverName + "   IP: " + myIp + "   Port: " + SERVER_PORT);
        tvMessages.setText(serverName + " (" + myIp + ") 進來了，等待 Client 連線...\n");

        // 開一條新的 thread 來跑 Server（不能在主執行緒跑）
        new Thread(new serverThread()).start();

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

        // 按離開：關掉聊天室頁（onDestroy 會鎖門並掛斷所有 Client）
        btnLeave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    // 接待員：一直站在門口，來一位就派一位服務生
    class serverThread implements Runnable {
        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(SERVER_PORT);
                while (true) {
                    Socket client = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(client);
                    clients.add(handler);
                    new Thread(handler).start();
                }
            } catch (IOException e) {
                e.printStackTrace();   // serverSocket 被關掉時會跳到這裡，代表 Server 打烊了
            }
        }
    }

    // 服務生：每位 Client 一位
    class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader reader;
        private OutputStreamWriter writer;
        private String tmp;
        private String name = "某人";   // 收到 join 之後才會知道真正的名字
        private boolean rejected = false;   // 是不是因為名字重複被拒絕

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            String ip = socket.getInetAddress().getHostAddress();
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new OutputStreamWriter(socket.getOutputStream());

                while ((tmp = reader.readLine()) != null) {
                    JSONObject jsonObj = new JSONObject(tmp);
                    String type = jsonObj.getString(MsgObj.LABEL_TYPE);
                    if (type.equals("join")) {
                        String wanted = jsonObj.getString(MsgObj.LABEL_NAME);
                        if (isNameTaken(wanted)) {
                            // 名字已經有人用了：告訴他被拒絕，然後結束這位客人的服務
                            send(msg.toJson("reject", "名字「" + wanted + "」已經有人使用，請換一個"));
                            rejected = true;
                            break;
                        }
                        name = wanted;   // 記住這位客人的名字
                    }
                    broadcast(tmp);              // 轉給所有人
                    showMessage(jsonObj, ip);    // 顯示在 Server 畫面
                }
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            } finally {
                // finally：不管上面是「正常結束」還是「出錯」，最後一定會來這裡
                clients.remove(this);   // 從點名簿劃掉
                close();                // 掛斷電話
                if (rejected) return;   // 被拒絕的人根本沒進來過，不用宣布離開
                try {
                    // 用這位客人的名字，幫他跟大家宣布離開
                    broadcast(new MsgObj(name).toJson("leave", ""));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                final String leftName = name;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvMessages.append(leftName + " 離開了\n");
                    }
                });
            }
        }

        // 把一行訊息傳給這位客人
        void send(String line) {
            if (writer == null) return;
            try {
                writer.write(line + "\n");
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 點名簿上有沒有「別人」已經用了這個名字
        boolean isNameTaken(String wanted) {
            for (ClientHandler c : clients) {
                if (c != this && c.name.equals(wanted)) {
                    return true;
                }
            }
            return false;
        }

        // 掛斷這位客人的電話
        void close() {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 把一行訊息傳給點名簿上的每一個人
    private void broadcast(String line) {
        for (ClientHandler c : clients) {
            c.send(line);
        }
    }

    // 依照種類，把訊息顯示在 Server 畫面上
    private void showMessage(final JSONObject jsonObj, final String ip) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    String type = jsonObj.getString(MsgObj.LABEL_TYPE);
                    String name = jsonObj.getString(MsgObj.LABEL_NAME);
                    if (type.equals("join")) {
                        tvMessages.append(name + " (" + ip + ") 進來了\n");
                    } else {
                        String text = jsonObj.getString(MsgObj.LABEL_MSG);
                        tvMessages.append(name + ": " + text + "\n");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // Server 自己按 Send
    private void sendMessage(final String text) {
        try {
            broadcast(msg.toJson("msg", text));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvMessages.append("Me: " + text + "\n");
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // 找出這支手機的 IPv4 位址
    private String getLocalIp() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "unknown";
    }

    // Server 關掉時：鎖門，並把每位客人的電話都掛掉
    @Override
    protected void onDestroy() {
        super.onDestroy();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (serverSocket != null) serverSocket.close();   // 鎖門，不再接新客人
                } catch (IOException e) {
                    e.printStackTrace();
                }
                for (ClientHandler c : clients) {
                    c.close();   // 每位客人都掛斷
                }
            }
        }).start();
    }
}