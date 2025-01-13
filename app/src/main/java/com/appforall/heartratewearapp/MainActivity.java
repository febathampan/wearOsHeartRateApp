package com.appforall.heartratewearapp;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PackageManagerCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.appforall.heartratewearapp.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
//WEear
public class MainActivity extends AppCompatActivity implements SensorEventListener, View.OnClickListener {

    private static final String HEARTBEAT_DATA_PATH = "/heartbeat_data";
    ActivityMainBinding mainBinding;

    private SensorManager sensorManager;
    private Sensor sensor;
    private JSONArray jsonArrayHeartBeats = new JSONArray();
    private static final String Graph_CAPABILITY_NAME = "graph_generation";


    /*
    OnCreate phase is when view is creating,
    OnResume - when app is in foreground state,
    onPause - when app is in destroy phase
    */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = mainBinding.getRoot();
        setContentView(view);
        init();
    }

    private void init() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BODY_SENSORS}, 5);
        }
        sensorManager = (SensorManager) getSystemService(getApplicationContext().SENSOR_SERVICE);
        if (sensorManager != null) {
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            showToast("In sensor changed method");
            float heartRate = event.values[0];
            HeartBeatRate rate = createRateDetail(null, heartRate);
            writeRateDetails(rate);
            mainBinding.txtHBValue.setText(String.valueOf(heartRate));
        }
    }

    //Write to sharedPreference
    private void writeRateDetails(HeartBeatRate heartRate) {
        if (heartRate != null) {
            SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("heartrate_details", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.putString(heartRate.getId(), String.valueOf(heartRate.getHeartBeatRate()));
            editor.commit();
        }
    }

    private HeartBeatRate createRateDetail(String id, float heartRate) {
        if (id == null) {
            id = String.valueOf(System.currentTimeMillis());
        }
        return new HeartBeatRate(id, heartRate);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

        switch (accuracy) {
            case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                showToast("High accuracy");
                break;
            case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                showToast("Medium accuracy");
                break;
            case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                showToast("Low accuracy");
                break;
            case SensorManager.SENSOR_STATUS_UNRELIABLE:
                showToast("Unreliable accuracy");
                break;
            default:
                showToast("Unknown accuracy status");
        }

    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }


    //If you want to use any sensors, it should be registered here
    @Override
    protected void onResume() {
        super.onResume();
        if (sensor != null) {
            showToast(sensor.getName());
            sensorManager.registerListener(this, this.sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensor != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == mainBinding.btnSendData.getId()) {
            sensorManager.unregisterListener(this);
            jsonArrayHeartBeats = readAllHeartBeatData();
            findCapabilityClient();
        }
    }

    private void findCapabilityClient() {
        Wearable.getCapabilityClient(this).getCapability(Graph_CAPABILITY_NAME, CapabilityClient.FILTER_REACHABLE)
                .addOnSuccessListener(capabilityInfo -> {
                    //capabilityInfo has reachable nodes with graph capability
                    findNodeForGraphGen(capabilityInfo);
                })
                .addOnFailureListener( e-> {
                    //Handle failure
                    e.printStackTrace();
                });
    }

    private void findNodeForGraphGen(CapabilityInfo capabilityInfo) {
        String graphNodeId = null;
        Set<Node> connectedNodes = capabilityInfo.getNodes();
        graphNodeId = pickBestNodeId(connectedNodes);
        if (graphNodeId == null) {
            showToast("No node found");
        } else {
            try {
                showToast(graphNodeId);
                byte[] heartBeatsData = jsonArrayHeartBeats.toString().getBytes();
                sendHeartBeatData(heartBeatsData, graphNodeId);
            } catch (Exception ex) {
                showToast(ex.getMessage());
            }
        }
    }
    private String pickBestNodeId(Set<Node> nodes) {
        String bestNodeId = null;
        // Find a nearby node or pick one arbitrarily.
        for (Node node : nodes) {
            if (node.isNearby()) {
                return node.getId();
            }
            bestNodeId = node.getId();
        }
        return bestNodeId;
    }
    private void sendHeartBeatData(byte[] dataToSend, String graphNodeId) {
        if (graphNodeId != null) {
            Task<Integer> sendTask =
                    Wearable.getMessageClient(this).sendMessage(
                            graphNodeId, HEARTBEAT_DATA_PATH, dataToSend
                    );
            sendTask.addOnSuccessListener(statusInfo -> {
                        // CapabilityInfo has the reachable nodes with the graph capability
                        showToast("Send Request Success");
                        clearValueStorage();
                    })
                    .addOnFailureListener(e -> {
                        showToast("Send Request Failed");
                    });
        } else {
            // Unable to retrieve node with transcription capability
        }
    }

    private void clearValueStorage() {
        SharedPreferences sharedPref = getSharedPreferences("heartrate_details", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.commit();
    }


    private JSONArray readAllHeartBeatData() {
        JSONArray jsonArray = new JSONArray();
        JSONObject obj = new JSONObject();
        SharedPreferences sharedPref = getSharedPreferences("heartrate_details", Context.MODE_PRIVATE);
        Map<String, ?> map = sharedPref.getAll();

        Set set = map.entrySet();
        Iterator itr = set.iterator();
        while (itr.hasNext()) {
            Map.Entry entry = (Map.Entry) itr.next();
            String savedHeartBeat = (String) entry.getValue();
            if (savedHeartBeat != null) {
                try {
                    obj.put(entry.getKey().toString(), savedHeartBeat);

                } catch (JSONException e) {
                    //throw new RuntimeException(e);
                }
                jsonArray.put(obj);
            }
        }
        return jsonArray;
    }
}