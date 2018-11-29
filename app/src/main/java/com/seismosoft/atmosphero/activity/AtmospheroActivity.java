package com.seismosoft.atmosphero.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.seismosoft.atmosphero.R;
import com.seismosoft.atmosphero.db.TinyDB;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;

public class AtmospheroActivity extends AppCompatActivity {

    String url = "";
    String TAG = "Atmosphero";
    TextView tv_Temperature;
    TextView tv_Humidity;
    TextView tv_Pressure;
    SwipeRefreshLayout swipeRefreshLayout;

    boolean is_c;
    boolean is_kpa;
    TinyDB tinydb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_atmosphero);

        tinydb = new TinyDB(getApplicationContext());

        if (!tinydb.getBoolean("NOTFIRSTRUN")) {
            tinydb.putBoolean("NOTFIRSTRUN", true);
            tinydb.putBoolean("IS_C", true);
            tinydb.putBoolean("IS_KPA", true);
            tinydb.putString("IP_ADDRESS", "192.168.1.103");
        }

        is_c = tinydb.getBoolean("IS_C");
        is_kpa = tinydb.getBoolean("IS_KPA");
        url = "http://" + tinydb.getString("IP_ADDRESS") + "/";

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshView);
        swipeRefreshLayout.setProgressViewOffset (false, 600, 800);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                sendWeatherRequest(null);
            }
        });

        tv_Temperature = (TextView) findViewById(R.id.textViewTemperatureValue);
        tv_Humidity = (TextView) findViewById(R.id.textViewHumidityValue);
        tv_Pressure = (TextView) findViewById(R.id.textViewPressureValue);

        tv_Temperature.setLongClickable(true);
        tv_Temperature.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (tinydb.getBoolean("IS_C") == true) {
                    is_c = false;
                    tinydb.putBoolean("IS_C", false);
                } else {
                    is_c = true;
                    tinydb.putBoolean("IS_C", true);
                }

                swipeRefreshLayout.setRefreshing(true);
                sendWeatherRequest(null);
                return true;
            }
        });

        tv_Pressure.setLongClickable(true);
        tv_Pressure.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (tinydb.getBoolean("IS_KPA") == true) {
                    is_kpa = false;
                    tinydb.putBoolean("IS_KPA", false);
                } else {
                    is_kpa = true;
                    tinydb.putBoolean("IS_KPA", true);
                }
                swipeRefreshLayout.setRefreshing(true);
                sendWeatherRequest(null);
                return true;
            }
        });


        sendWeatherRequest(null);
    }

    public void sendWeatherRequest(View view) {

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONObject data = response.getJSONObject("data");
                    float temp = Float.parseFloat(data.getString("temperature"));
                    float humi = Float.parseFloat(data.getString("humidity"));
                    float pres = Float.parseFloat(data.getString("pressure"));

                    DecimalFormat df = new DecimalFormat("##.##");

                    if (is_c == false) {
                        temp = (temp * 9 / 5) + 32.00f;
                        tv_Temperature.setText(temp + " " + "\u00B0" + "F");
                    } else {
                        tv_Temperature.setText(temp + " " + "\u00B0" + "C");
                    }

                    tv_Humidity.setText(df.format(humi) + " " + "%");

                    if (is_kpa == true) {
                        pres = pres / 1000.00f;
                        tv_Pressure.setText(df.format(pres) + " " + "KPa");
                    } else {
                        tv_Pressure.setText(df.format(pres) + " " + "Pa");
                    }

                    swipeRefreshLayout.setRefreshing(false);

                } catch (JSONException e) {
                    e.printStackTrace();
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, error.toString());
                Toast.makeText(AtmospheroActivity.this, "Could not connect to Weather Station", Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        queue.add(jsonObjectRequest);
    }

    public void show_settings(View view) {
        LayoutInflater inflater = getLayoutInflater();
        View settingsLayout = inflater.inflate(R.layout.settings_popup, null);

        final AlertDialog.Builder settingsWidow = new AlertDialog.Builder(this);
        settingsWidow.setView(settingsLayout);
        settingsWidow.setCancelable(true);

        final AlertDialog dialog = settingsWidow.create();

        final EditText editTextIpAddress = (EditText) settingsLayout.findViewById(R.id.editTextViewIpAddressValue);
        final RadioButton radioButtonCelsius = (RadioButton) settingsLayout.findViewById(R.id.radioButtonSelectCelsius);
        final RadioButton radioButtonFarenheit = (RadioButton) settingsLayout.findViewById(R.id.radioButtonSelectFarenheit);
        final RadioButton radioButtonPascal = (RadioButton) settingsLayout.findViewById(R.id.radioButtonSelectPascals);
        final RadioButton radioButtonKiloascal = (RadioButton) settingsLayout.findViewById(R.id.radioButtonSelectKilopascals);
        final Button buttonSaveSettings = (Button)  settingsLayout.findViewById(R.id.buttonSaveSettings);

        editTextIpAddress.setText(tinydb.getString("IP_ADDRESS"));

        if(tinydb.getBoolean("IS_C") == true) {
            radioButtonCelsius.setChecked(true);
        } else {
            radioButtonFarenheit.setChecked(true);
        }

        if (tinydb.getBoolean("IS_KPA") == true) {
            radioButtonKiloascal.setChecked(true);
        } else {
            radioButtonPascal.setChecked(true);
        }

        buttonSaveSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(radioButtonCelsius.isChecked()) {
                    tinydb.putBoolean("IS_C", true);
                    is_c = true;
                } else {
                    tinydb.putBoolean("IS_C", false);
                    is_c = false;
                }

                if(radioButtonKiloascal.isChecked()) {
                    tinydb.putBoolean("IS_KPA", true);
                    is_kpa = true;
                } else {
                    tinydb.putBoolean("IS_KPA", false);
                    is_kpa = false;
                }

                tinydb.putString("IP_ADDRESS", editTextIpAddress.getText().toString());
                url = "http://" + editTextIpAddress.getText().toString() +"/";
                swipeRefreshLayout.setRefreshing(true);
                sendWeatherRequest(null);
                dialog.dismiss();
            }
        });
        dialog.show();
    }
}
