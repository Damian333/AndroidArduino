package com.example.thesispurpose;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

public class MainActivity extends AppCompatActivity {

    private LinearLayout containerLayout;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        containerLayout = findViewById(R.id.container_layout);

        fetchData();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                fetchData();
                handler.postDelayed(this, 5000);
            }
        };

        handler.postDelayed(runnable, 5000);
    }

    private void fetchData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://esparduino-6b7c1e00c602.herokuapp.com/getdata");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");

                    InputStream inputStream = connection.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line);
                    }

                    final String response = stringBuilder.toString();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                JSONObject jsonObject = new JSONObject(response);
                                renderData(jsonObject);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    bufferedReader.close();
                    inputStream.close();
                    connection.disconnect();

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void renderData(JSONObject data) {
        try {
            JSONArray arduinoData = data.getJSONArray("arduinoData");

            // Check if there is at least one object in the array
            if (arduinoData.length() > 0) {
                // Get the last object from the array
                JSONObject lastObject = arduinoData.getJSONObject(arduinoData.length() - 1);

                Map<String, Object> pairs = new HashMap<>();
                Iterator<String> keys = lastObject.keys();

                while (keys.hasNext()) {
                    String key = keys.next();
                    Object value = lastObject.get(key);
                    pairs.put(key, value);
                }

                containerLayout.removeAllViews(); // Clear existing views before adding new ones

                int index = 0; // Index for selecting container color

                LinearLayout rowLayout = null; // Initialize a rowLayout

                for (Map.Entry<String, Object> entry : pairs.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();

                    if (rowLayout == null || rowLayout.getChildCount() == 2) {
                        // Create a new rowLayout if the current one is null or has two containers
                        rowLayout = new LinearLayout(this);
                        rowLayout.setOrientation(LinearLayout.HORIZONTAL);

                        // Set layout parameters for the rowLayout
                        LinearLayout.LayoutParams rowLayoutParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        rowLayoutParams.setMargins(0, 16, 0, 16); // Add margin between rows
                        rowLayoutParams.gravity = Gravity.CENTER; // Center the rowLayout
                        rowLayout.setLayoutParams(rowLayoutParams);

                        containerLayout.addView(rowLayout); // Add the new rowLayout to the containerLayout
                    }

                    LinearLayout container = new LinearLayout(this);
                    container.setOrientation(LinearLayout.VERTICAL);

                    // Set background drawable with rounded corners
                    Drawable drawable = getResources().getDrawable(R.drawable.rounded_corners);
                    container.setBackground(drawable);

                    // Set layout parameters for the container
                    LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                            0, // Width set to 0 for weight
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1.0f // Weight set to 1 for equal distribution in the row
                    );
                    container.setLayoutParams(containerParams);
                    container.setGravity(Gravity.CENTER); // Center the container
                    container.setPadding(8,8,8,8); // Add padding to the container

                    TextView keyTextView = new TextView(this);
                    keyTextView.setText(key);
                    keyTextView.setTextColor(getResources().getColor(R.color.red));
                    keyTextView.setTextSize(12); // Set text size for the first element

                    TextView valueTextView = new TextView(this);
                    valueTextView.setText(value.toString() + getUnit(key));
                    valueTextView.setTextColor(getResources().getColor(R.color.blue));
                    valueTextView.setTextSize(18); // Set text size for the second element

                    // Center text in each TextView
                    keyTextView.setGravity(Gravity.CENTER);
                    valueTextView.setGravity(Gravity.CENTER);

                    container.addView(keyTextView);
                    container.addView(valueTextView);

                    container.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // When a block is clicked, navigate to the DetailsActivity
                            Intent intent = new Intent(MainActivity.this, DetailsActivity.class);
                            intent.putExtra("selectedBlock", key); // Pass the selected block to the next screen
                            startActivity(intent);
                        }
                    });

                    rowLayout.addView(container); // Add the container to the current rowLayout
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    // Helper method to get the unit for a specific key
    private String getUnit(String key) {
        switch (key) {
            case "PM25":
            case "PM10":
            case "PM1":
                return " ug/m³";
            case "Light":
                return " mV";
            case "Temperature":
                return " °C";
            case "Pressure":
                return " hPa";
            case "Humidity":
                return " RH";
            case "eTVOC":
                return " ppb";
            case "eCO2":
                return " ppm";
            default:
                return "";
        }
    }
}