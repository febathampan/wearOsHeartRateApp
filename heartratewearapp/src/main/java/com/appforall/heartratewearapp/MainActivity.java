package com.appforall.heartratewearapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.appforall.heartratewearapp.databinding.ActivityMainBinding;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements MessageClient.OnMessageReceivedListener {
    ActivityMainBinding mainBinding;
    private static final String HEARTBEAT_DATA_PATH = "/heartbeat_data";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        View v = mainBinding.getRoot();
        setContentView(v);
        init();
    }

    private void init() {
        Wearable.getMessageClient(this).addListener(this);
    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(HEARTBEAT_DATA_PATH)) {
            showToast("Message Received");
            byte[] data = messageEvent.getData();
            listHeartBeatData(data);
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void listHeartBeatData(byte[] jsonArrayHeartBeats) {
        String jsonString = new String(jsonArrayHeartBeats);
        try {
            // Parse the string as a JSONArray
            JSONArray jsonArray = new JSONArray(jsonString);

            // Iterate through the JSONArray
            for (int i = 0; i < jsonArray.length(); i++) {
                // Access elements using jsonArray.getJSONObject(i)
                JSONObject heartBeatObject = jsonArray.getJSONObject(i);

                // Use the names() method to get the keys
                JSONArray names = heartBeatObject.names();
                for (int j = 0; j < names.length(); j++) {
                    String key = names.getString(j);
                    int value = heartBeatObject.getInt(key);

                    // Log the time and heartbeat
                    Log.i("info", "Time: " + key + ", Heart Beat: " + String.valueOf(value));
                }
            }
        } catch (JSONException e) {
            // Handle JSON parsing errors
            Log.e("WearableReceiver", "Error parsing JSON: " + e.getMessage());
        }
    }

}