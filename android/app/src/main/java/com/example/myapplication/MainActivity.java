package com.example.myapplication;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class MainActivity extends AppCompatActivity {
    private SwitchCompat switchCompatPN,switchCompatPK,switchCompatNS, switchCompatCong;

    private SwitchCompat switchTC; // SwitchCompat ở trên (AUTO/HAND)
    private ImageView imageViewPN, imageViewPK, bgr_main;
    private static final int SPEECH_REQUEST_CODE = 100;
    private OkHttpClient client;
    private WebSocket ws;
    private EditText edtTime;
    private int hour, minute;
    private boolean isTimeSet = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        Button btnVoiceSearch = findViewById(R.id.btnVoiceSearch);
        client = new OkHttpClient();
        start();
        bgr_main = findViewById(R.id.bgr_main);
        // Lấy đối tượng Calendar hiện tại
        Calendar calendar = Calendar.getInstance();
        // Lấy thời gian hiện tại
        int hour = calendar.get(Calendar.HOUR_OF_DAY); // Giờ theo định dạng 24 giờ
        if(hour>=18 || hour<=5){
            bgr_main.setBackgroundResource(R.drawable.dem);
        } else {
            bgr_main.setBackgroundResource(R.drawable.sky);
        }

        switchTC = findViewById(R.id.nutTC);
        switchCompatPN = findViewById(R.id.nutPN);
        switchCompatPK = findViewById(R.id.nutPK);
        switchCompatNS = findViewById(R.id.nutNS);
        switchCompatCong = findViewById(R.id.nut_cong);
        imageViewPN = findViewById(R.id.pn);
        imageViewPK = findViewById(R.id.pk);
        edtTime = findViewById(R.id.edtTime);

        minute = calendar.get(Calendar.MINUTE);

        edtTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePickerDialog();
            }
        });

        switchCompatNS.setEnabled(false);


// Lấy trạng thái ban đầu của switchCompatPN và switchCompatPK
        boolean checkpnBANDAU = switchCompatPN.isChecked();
        boolean checkpkBANDAU = switchCompatPK.isChecked();

// Cập nhật nền ban đầu cho imageViewPN và imageViewPK dựa trên trạng thái ban đầu của SwitchCompat
        updateBackground(checkpnBANDAU, imageViewPN, R.drawable.pn, R.drawable.pn_off);
        updateBackground(checkpkBANDAU, imageViewPK, R.drawable.pk, R.drawable.pk_off);

        // Lắng nghe sự kiện khi trạng thái của switch thủ công thay đổi
        switchTC.setOnCheckedChangeListener(new SwitchCompat.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Nếu switchTC được chọn là "AUTO"
                if (isChecked) {
                    // Đóng băng switchNS (ON/OFF)
                    switchCompatNS.setEnabled(true); // Không thể bấm
                    if(hour>=18 && hour<=5){
                        //Bật đèn ngoài sân
                    }
                } else {
                    // Nếu switchTC được chọn là "HAND"
                    switchCompatNS.setEnabled(false); // Có thể bấm
                }
            }
        });
        btnVoiceSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startVoiceSearch();
            }
        });

        //đèn phòng ngủ
        switchCompatPN.setOnCheckedChangeListener(new SwitchCompat.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Gọi lại updateBackground khi trạng thái của switchCompatPN thay đổi
                updateBackground(isChecked, imageViewPN, R.drawable.pn, R.drawable.pn_off);
            }
        });


        //đèn phòng khách
        switchCompatPK.setOnCheckedChangeListener(new SwitchCompat.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Gọi lại updateBackground khi trạng thái của switchCompatPK thay đổi
                updateBackground(isChecked, imageViewPK, R.drawable.pk, R.drawable.pk_off);
            }
        });


        switchCompatNS.setOnCheckedChangeListener(new SwitchCompat.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //Bật tắt ngoài sân nè
            }
        });

        switchCompatCong.setOnCheckedChangeListener(new SwitchCompat.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //mở tắt cổng nè
            }
        });



    }

    private void showTimePickerDialog() {
        Calendar currentCalendar = Calendar.getInstance();

       int hour = currentCalendar.get(Calendar.HOUR_OF_DAY);

        if(switchCompatPN.isChecked()){
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    this,
                    android.R.style.Theme_Holo_Light_Dialog,

                    new TimePickerDialog.OnTimeSetListener() {

                        @Override
                        public void onTimeSet(TimePicker view, int hour, int minute) {
                            // Do something with the selected time
                            final String time = String.format("%02d:%02d", hour, minute);
                            edtTime.setText(time);

                            // Create a handler
                            final Handler handler = new Handler();
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    // Get current time
                                    Calendar currentCalendar = Calendar.getInstance();
                                    String currentTime = String.format("%02d:%02d", currentCalendar.get(Calendar.HOUR_OF_DAY), currentCalendar.get(Calendar.MINUTE));

                                    // Compare current time with set time
                                    if (currentTime.equals(time)) {
                                        edtTime.setText("set time");
                                        switchCompatPN.setChecked(false);
                                        JSONObject jsonObject = new JSONObject();

                                        try {
                                            jsonObject.put("ledStatus", "LED_OFF_PN");


                                            // Convert the JSONObject to a string
                                            String jsonString = jsonObject.toString();

                                            // Send the JSON string
                                            ws.send(jsonString);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        handler.removeCallbacks(this);  // Stop checking after the time matches
                                    } else {
                                        handler.postDelayed(this, 0);
                                    }
                                }
                            });
                        }
                    },
                    hour,
                    minute,
                    true
            );

            timePickerDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            timePickerDialog.show();

        } else {
            // Assuming 'edtTime' is your EditText
            edtTime.setEnabled(true);
        }

    }

    // Phương thức updateBackground để cập nhật nền của imageView dựa trên trạng thái của SwitchCompat
    private void updateBackground ( boolean isChecked, ImageView imageView,int drawableOn, int drawableOff){
        if (isChecked) {
            // Nếu SwitchCompat được kiểm tra (ON), thiết lập hình nền khi ON cho imageView
            imageView.setBackgroundResource(drawableOn); // Sử dụng hình nền khi ON
        } else {
            // Nếu SwitchCompat không được kiểm tra (OFF), thiết lập hình nền khi OFF cho imageView
            imageView.setBackgroundResource(drawableOff); // Sử dụng hình nền khi OFF
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.Home) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent); // Bắt đầu Activity mới
            finish();
            return true;
        }
        else{
            Intent intent1 = new Intent(this, chart.class);
            startActivity(intent1); // Bắt đầu Activity mới
            finish();
            return true;
        }
    }


    public void startVoiceSearch() {
        // Bắt đầu Intent nhận dạng giọng nói
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Bật/Tắt tầng..., ngoài sân, hoặc Mở/Đóng cửa");

        // Kiểm tra xem thiết bị của bạn có hỗ trợ nhận dạng giọng nói không
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } else {
            Toast.makeText(this, "Your device does not support speech recognition", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && results.size() > 0) {
                String result = results.get(0); // Lấy kết quả đầu tiên
                TextSubmit(result);
            }
        }
    }


    private void start() {
        Request request = new Request.Builder().url("ws://192.168.8.68:3000/ws").build();
        EchoWebSocketListener listener = new EchoWebSocketListener();
        ws = client.newWebSocket(request, listener);
        client.dispatcher().executorService().shutdown();
    }
    private final class EchoWebSocketListener extends WebSocketListener {
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            runOnUiThread(() -> {
                switchCompatCong.setOnClickListener(v -> {
                    JSONObject jsonObject = new JSONObject();
                    try {
                        // Put the LED status in the JSONObject
                        if (switchCompatCong.isChecked()) {
                            jsonObject.put("doorStatus", "DOOR_OPEN");
                        } else if(switchCompatCong.isChecked()==false){
                            jsonObject.put("doorStatus", "DOOR_CLOSE");
                        }

                        // Convert the JSONObject to a string
                        String jsonString = jsonObject.toString();

                        // Send the JSON string
                        ws.send(jsonString);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(MainActivity.this, "Connected to the server", Toast.LENGTH_SHORT).show();
                });
                switchCompatPN.setOnClickListener(v -> {
                            JSONObject jsonObject = new JSONObject();
                            try {
                                // Put the LED status in the JSONObject
                                if (switchCompatPN.isChecked()) {
                                    jsonObject.put("ledStatus", "LED_ON_PN");
                                } else if(switchCompatPK.isChecked()==false){
                                    jsonObject.put("ledStatus", "LED_OFF_PN");
                                }

                                // Convert the JSONObject to a string
                                String jsonString = jsonObject.toString();

                                // Send the JSON string
                                ws.send(jsonString);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            Toast.makeText(MainActivity.this, "Connected to the server", Toast.LENGTH_SHORT).show();
                        });
                switchCompatPK.setOnClickListener(v -> {
                    JSONObject jsonObject = new JSONObject();
                    try {
                        // Put the LED status in the JSONObject
                        if (switchCompatPK.isChecked()) {
                            jsonObject.put("ledStatus", "LED_ON_PK");
                        } else if(switchCompatPK.isChecked()==false){
                            jsonObject.put("ledStatus", "LED_OFF_PK");
                        }

                        // Convert the JSONObject to a string
                        String jsonString = jsonObject.toString();

                        // Send the JSON string
                        ws.send(jsonString);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(MainActivity.this, "Connected to the server", Toast.LENGTH_SHORT).show();
                });
                switchCompatNS.setOnClickListener(v -> {
                    JSONObject jsonObject = new JSONObject();
                    try {
                        // Put the LED status in the JSONObject
                        if (switchCompatNS.isChecked()) {
                            jsonObject.put("ledStatus", "LED_ON_NN");
                        } else if(switchCompatNS.isChecked()==false){
                            jsonObject.put("ledStatus", "LED_OFF_NN");
                        }

                        // Convert the JSONObject to a string
                        String jsonString = jsonObject.toString();

                        // Send the JSON string
                        ws.send(jsonString);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(MainActivity.this, "Connected to the server", Toast.LENGTH_SHORT).show();
                });
                switchTC.setOnClickListener(v -> {
                    JSONObject jsonObject = new JSONObject();
                    try {
                        // Put the LED status in the JSONObject
                        if (switchTC.isChecked()) {
                            jsonObject.put("ledStatus", "MANUAL");
                        } else if(switchTC.isChecked()==false){
                            jsonObject.put("ledStatus", "AUTO");
                        }

                        // Convert the JSONObject to a string
                        String jsonString = jsonObject.toString();

                        // Send the JSON string
                        ws.send(jsonString);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(MainActivity.this, "Connected to the server", Toast.LENGTH_SHORT).show();
                });
            });
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            runOnUiThread(() -> {
                try {
                    JSONObject jsonObject = new JSONObject(text);
                    if (jsonObject.has("doorStatus")) {
//                        sensorValue.setText(jsonObject.getString("sensorValue"));
                        if ("DOOR_OPEN".equals(jsonObject.getString("doorStatus"))) {
                            switchCompatCong.setChecked(true);
                        }else if ("DOOR_CLOSE".equals(jsonObject.getString("doorStatus"))) {
                            switchCompatCong.setChecked(false);}
                    }
                    if (jsonObject.has("sensorValue")) {
//                        sensorValue.setText(jsonObject.getString("sensorValue"));
                    }
                    if (jsonObject.has("ledStatus")) { // Kiểm tra xem trạng thái LED có được gửi từ server hay không
                        if ("LED_ON_PK".equals(jsonObject.getString("ledStatus"))) {
                            switchCompatPK.setChecked(true); // Nếu trạng thái LED là 'LED_ON', bật đèn LED
                        } else if ("LED_OFF_PK".equals(jsonObject.getString("ledStatus"))) {
                            switchCompatPK.setChecked(false); // Nếu trạng thái LED là 'LED_OFF', tắt đèn LED
                        }else if ("LED_ON_PN".equals(jsonObject.getString("ledStatus"))) {
                            switchCompatPN.setChecked(true);
                        }else if ("LED_OFF_PN".equals(jsonObject.getString("ledStatus"))) {
                            switchCompatPN.setChecked(false);
                        }else if ("LED_ON_NN".equals(jsonObject.getString("ledStatus"))) {
                            switchCompatNS.setChecked(true);
                        }else if ("LED_OFF_NN".equals(jsonObject.getString("ledStatus"))) {
                            switchCompatNS.setChecked(false);
                        }else if ("MANUAL".equals(jsonObject.getString("ledStatus"))) {
                            switchTC.setChecked(true);
                            switchCompatNS.setEnabled(true);
                        }else if ("AUTO".equals(jsonObject.getString("ledStatus"))) {
                            switchTC.setChecked(false);
                            switchCompatNS.setEnabled(false);
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
    public void TextSubmit(String command) {
        // Process the recognized text command here
        if (command != null) {
            if (command.contains("bật đèn phòng khách") || command.contains("mở đèn phòng khách") || command.contains("turn on living room light") ) {
                updateBackground(true, imageViewPK, R.drawable.pk, R.drawable.pk_off);
                switchCompatPK.setChecked(true);
                JSONObject jsonObject = new JSONObject();
                try{
                    jsonObject.put("ledStatus", "LED_ON_PK");
                    String jsonString = jsonObject.toString();
                    ws.send(jsonString);
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (command.contains("tắt đèn phòng khách") || command.contains("đóng đèn phòng khách") || command.contains("turn off living room light") || command.contains("đống đèn phòng khách") || command.contains("Đóng đèn phòng khách") || command.contains("Đống đèn phòng khách")|| command.contains("Tắt đèn phòng khách")) {
                updateBackground(false, imageViewPK, R.drawable.pk, R.drawable.pk_off);
                switchCompatPK.setChecked(false);
                JSONObject jsonObject = new JSONObject();
                try{
                    jsonObject.put("ledStatus", "LED_OFF_PK");
                    String jsonString = jsonObject.toString();
                    ws.send(jsonString);
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (command.contains("bật đèn phòng ngủ") || command.contains("mở đèn phòng ngủ")|| command.contains("turn on bedroom light")) {
                updateBackground(true, imageViewPN, R.drawable.pn, R.drawable.pn_off);
                switchCompatPN.setChecked(true);
                JSONObject jsonObject = new JSONObject();
                try{
                    jsonObject.put("ledStatus", "LED_ON_PN");
                    String jsonString = jsonObject.toString();
                    ws.send(jsonString);
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (command.contains("tắt đèn phòng ngủ") || command.contains("đóng đèn phòng ngủ")|| command.contains("turn off bedroom light") || command.contains("Tắt đèn phòng ngủ") ) {
                updateBackground(false, imageViewPN, R.drawable.pn, R.drawable.pn_off);
                switchCompatPN.setChecked(false);
                JSONObject jsonObject = new JSONObject();
                try{
                    jsonObject.put("ledStatus", "LED_OFF_PN");
                    String jsonString = jsonObject.toString();
                    ws.send(jsonString);
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (command.contains("mở cửa") || command.contains("mở cổng")) {
                switchCompatCong.setChecked(true);
                JSONObject jsonObject = new JSONObject();
                try{
                    jsonObject.put("doorStatus", "DOOR_OPEN");
                    String jsonString = jsonObject.toString();
                    ws.send(jsonString);
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (command.contains("đóng cửa") || command.contains("đóng cổng")) {
                switchCompatCong.setChecked(false);
                JSONObject jsonObject = new JSONObject();
                try{
                    jsonObject.put("doorStatus", "DOOR_CLOSE");
                    String jsonString = jsonObject.toString();
                    ws.send(jsonString);
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (command.contains("bật đèn ngoài sân") || command.contains("mở đèn ngoài sân") || command.contains("turn on outdoor light")) {
                if (switchTC.isChecked()==false) {
                    Toast.makeText(this, "Chế độ tự động đang được bật, tắt chế độ tự động để thực hiện", Toast.LENGTH_SHORT).show();
                } else {
                    switchCompatNS.setChecked(true);
                    JSONObject jsonObject = new JSONObject();
                    try{
                        jsonObject.put("ledStatus", "LED_ON_NN");
                        String jsonString = jsonObject.toString();
                        ws.send(jsonString);
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } else if (command.contains("tắt đèn ngoài sân")) {
                if (switchTC.isChecked()==false) {
                    Toast.makeText(this, "Chế độ tự động đang được bật, tắt chế độ tự động để thực hiện", Toast.LENGTH_SHORT).show();
                } else {
                    switchCompatNS.setChecked(false);
                    JSONObject jsonObject = new JSONObject();
                    try{
                        jsonObject.put("ledStatus", "LED_OFF_NN");
                        String jsonString = jsonObject.toString();
                        ws.send(jsonString);
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                // Command not recognized
                Toast.makeText(this, "Không hiểu lệnh! Vui lòng không nhập nhiều lệnh cùng một lúc", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    public boolean onCreatePanelMenu(int featureId, @NonNull Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu,menu);
        return true;
    }
}
