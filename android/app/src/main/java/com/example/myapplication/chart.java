package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class chart extends AppCompatActivity {
    private OkHttpClient client;

    ArrayList<History> listhis;
    MyAdapterHistory myAdapterHistory;

    String status[] = {"success", "Fail", "Success"};
    String time[] = {"success", "Fail", "Success"};
    ListView listView;

    private String responseData;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chart);

        listView = findViewById(R.id.lvhistory);
        listhis = new ArrayList<>();

        for(int i=0; i<status.length;i++){
            listhis.add(new History(status[i],time[i]));
        }
        myAdapterHistory = new MyAdapterHistory(chart.this,R.layout.history_item, listhis);
        listView.setAdapter(myAdapterHistory);

        client = new OkHttpClient();

        LineChart chart1 = findViewById(R.id.chart);

        String url = "http://192.168.103.191:3000/api/led-operation-time";

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
                                            @Override
                                            public void onFailure(Call call, IOException e) {
                                                e.printStackTrace();
                                            }

                                            @Override
                                            public void onResponse(Call call, Response response) throws IOException {
                                                if (!response.isSuccessful()) {
                                                    throw new IOException("Unexpected code " + response);
                                                } else {
                                                    // Do something with the response
                                                    String responseData = response.body().string();
                                                    try {
                                                        JSONArray jsonArray = new JSONArray(responseData);
                                                        ArrayList<Entry> ledPKData = new ArrayList<>();
                                                        ArrayList<Entry> ledPNData = new ArrayList<>();


                                                        // Get the current date
                                                        LocalDate now = null;
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                            now = LocalDate.now();
                                                        }
                                                        // Get the date a week ago
                                                        LocalDate oneWeekAgo = null;
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                            oneWeekAgo = now.minusWeeks(1);
                                                        }

                                                        for (int i = 0; i < jsonArray.length(); i++) {
                                                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                                                            String ledType = jsonObject.getJSONObject("_id").getString("ledType");
                                                            int year = jsonObject.getJSONObject("_id").getInt("year");
                                                            int month = jsonObject.getJSONObject("_id").getInt("month");
                                                            int day = jsonObject.getJSONObject("_id").getInt("day");
                                                            LocalDate date = null;
                                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                                date = LocalDate.of(year, month, day);
                                                            }

                                                            // Check if the date is within the last week
                                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                                if (!date.isBefore(oneWeekAgo)) {
                                                                    float totalOperationTime = (float) jsonObject.getDouble("totalOperationTime");

                                                                    // Convert totalOperationTime from milliseconds to minutes
                                                                    totalOperationTime = totalOperationTime / 60000;

                                                                    if (ledType.equals("LED_PK")) {
                                                                        ledPKData.add(new Entry(i, totalOperationTime, date));
                                                                    } else if (ledType.equals("LED_PN")) {
                                                                        ledPNData.add(new Entry(i, totalOperationTime, date));
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        LineDataSet setPK = new LineDataSet(ledPKData, "Thời gian sử dụng đèn phòng ngủ (phút)");
                                                        setPK.setColor(Color.BLUE);
                                                        setPK.setLineWidth(2f);

                                                        LineDataSet setPN = new LineDataSet(ledPNData, "Thời gian sử dụng đèn phòng khách (phút)");
                                                        setPN.setColor(Color.RED);
                                                        setPN.setLineWidth(2f);

                                                        final ArrayList<ILineDataSet> dataSets = new ArrayList<>();
                                                        dataSets.add(setPK);
                                                        dataSets.add(setPN);


                                                        runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                LineData data = new LineData(dataSets);
                                                                chart1.setData(data);
                                                                chart1.invalidate(); // refresh chart
                                                            }
                                                        });

                                                    } catch (JSONException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            }
                                        });

    }
    @Override
    public boolean onCreatePanelMenu(int featureId, @NonNull Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu,menu);
        return true;
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
}