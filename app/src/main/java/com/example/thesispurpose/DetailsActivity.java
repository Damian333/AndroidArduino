package com.example.thesispurpose;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.thesispurpose.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DetailsActivity extends AppCompatActivity {

    private LineChart lineChart;
    private TextView detailsTextView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        Toolbar toolbar = findViewById(R.id.detailsToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        detailsTextView = findViewById(R.id.detailsTextView);
        lineChart = findViewById(R.id.lineChart);

        // Get the data passed from the previous screen
        String selectedBlock = getIntent().getStringExtra("selectedBlock");

        // Display the selected block in the Toolbar
        setTitle("Wybrany parametr: " + selectedBlock);

        // Buttons for different time periods
        Button btnHour = findViewById(R.id.hourButton);
        Button btnDay = findViewById(R.id.dayButton);
        Button btnThreeDays = findViewById(R.id.threeDaysButton);

        // Set click listeners for the buttons
        btnHour.setOnClickListener(view -> fetchDataForTimePeriod("hour"));
        btnDay.setOnClickListener(view -> fetchDataForTimePeriod("day"));
        btnThreeDays.setOnClickListener(view -> fetchDataForTimePeriod("3days"));
    }

    public static String convertTimestampToDateTime(long timestamp) {
        // Create a SimpleDateFormat object with the desired date and time format
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        // Convert the timestamp to Date object
        Date date = new Date(timestamp);

        // Format the Date object to a human-readable string
        return dateFormat.format(date);
    }

    private void fetchDataForTimePeriod(String timePeriod) {
        // Get the data passed from the previous screen
        String selectedParameter = getIntent().getStringExtra("selectedBlock");

        if (selectedParameter != null && !selectedParameter.isEmpty()) {
            // Formulate the URL based on the selected time period and parameter
            String apiUrl = "https://esparduino-6b7c1e00c602.herokuapp.com/getData/" + selectedParameter + "/" + timePeriod;
            new FetchData().execute(apiUrl);
        } else {
            // Handle the case where selectedParameter is not available
            Log.e("DetailsActivity", "Parameter not available");
        }
    }

    private class FetchData extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            if (params.length == 0) {
                return null;
            }

            String apiUrl = params[0];
            try {
                URL url = new URL(apiUrl);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                try {
                    InputStream in = urlConnection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder result = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }

                    return result.toString();
                } finally {
                    urlConnection.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (result != null) {
                try {
                    // Log the received JSON data
                    Log.d("JSON_DATA", "Received JSON data: " + result);

                    JSONObject jsonObject = new JSONObject(result);

                    // Define parameters
                    List<String> parameterNames = Arrays.asList("Temperature", "Pressure", "Humidity", "PM25", "PM10", "PM1", "Light", "eTVOC", "eCO2");

                    // Create a list to hold LineDataSets
                    ArrayList<LineDataSet> dataSets = new ArrayList<>();

                    // Iterate through parameters
                    for (String paramName : parameterNames) {
                        ArrayList<Entry> entries = new ArrayList<>();

                        // Check if the parameter exists in the JSON data
                        if (jsonObject.has(paramName)) {
                            // Extract the parameter array
                            JSONArray parameterArray = jsonObject.getJSONArray(paramName);

                            // Iterate through the array and create entries
                            for (int i = 0; i < parameterArray.length(); i++) {
                                JSONObject dataPoint = parameterArray.getJSONObject(i);

                                // Check if the timestamp exists in the current data point
                                if (dataPoint.has("timestamp")) {
                                    long timestamp = dataPoint.getLong("timestamp");
                                    Log.d("JSON_DATA", "TIMESTAPMY: " + timestamp);
                                    String timestampString = convertTimestampToDateTime(timestamp);
                                    Log.d("JSON_DATA", "TIMESTAPMY: " + timestampString);
                                    float value = (float) dataPoint.getDouble("value");

                                    entries.add(new Entry(timestamp, value));
                                }
                            }

                            // Create LineDataSet for the parameter
                            LineDataSet dataSet = new LineDataSet(entries, paramName);
                            dataSets.add(dataSet);
                        }
                    }

                    // Create LineData from the list of LineDataSets
                    LineData lineData = new LineData(dataSets.toArray(new LineDataSet[0]));

                    // Get the LineChart
                    LineChart lineChart = findViewById(R.id.lineChart);

                    // Set LineData to LineChart
                    lineChart.setData(lineData);
                    lineChart.invalidate(); // Refresh the chart
                    // Continue with the rest of your code...
                } catch (JSONException e) {
                    e.printStackTrace();
                    detailsTextView.setText("Error parsing data: " + e.getMessage());
                }
            } else {
                detailsTextView.setText("Error fetching data");
            }
        }

    }
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
