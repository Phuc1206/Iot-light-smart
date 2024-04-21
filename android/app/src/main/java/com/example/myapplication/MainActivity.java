package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class MainActivity extends AppCompatActivity {
    private ToggleButton button;
    private TextView sensorValue;
    private OkHttpClient client;
    private WebSocket ws;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        button = findViewById(R.id.button);
        sensorValue = findViewById(R.id.sensorValue);

        client = new OkHttpClient();
        start();
    }

    private void start() {
        Request request = new Request.Builder().url("ws://192.168.138.191:3000/ws").build();
        EchoWebSocketListener listener = new EchoWebSocketListener();
        ws = client.newWebSocket(request, listener);
        client.dispatcher().executorService().shutdown();
    }
    private final class EchoWebSocketListener extends WebSocketListener {
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            runOnUiThread(() -> {

                button.setOnClickListener(v -> {
                    if (button.isChecked()) {
                        ws.send("LED_ON");
                    } else {
                        ws.send("LED_OFF");
                    }
                });
                Toast.makeText(MainActivity.this, "Connected to the server", Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            runOnUiThread(() -> {
                try {
                    JSONObject jsonObject = new JSONObject(text);
                    if (jsonObject.has("sensorValue")) {
                        sensorValue.setText(jsonObject.getString("sensorValue"));
                    }
                    if (jsonObject.has("ledStatus")) { // Kiểm tra xem trạng thái LED có được gửi từ server hay không
                        if ("LED_ON".equals(jsonObject.getString("ledStatus"))) {
                            button.setChecked(true); // Nếu trạng thái LED là 'LED_ON', bật đèn LED
                        } else if ("LED_OFF".equals(jsonObject.getString("ledStatus"))) {
                            button.setChecked(false); // Nếu trạng thái LED là 'LED_OFF', tắt đèn LED
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            onMessage(webSocket, bytes.utf8());
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            webSocket.close(1000, null);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            t.printStackTrace();
        }
    }
}