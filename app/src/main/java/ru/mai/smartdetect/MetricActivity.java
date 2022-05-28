package ru.mai.smartdetect;

import static java.lang.Thread.sleep;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import com.hivemq.client.mqtt.mqtt3.message.subscribe.suback.Mqtt3SubAck;

import java.nio.ByteBuffer;
import java.util.UUID;

public class MetricActivity extends AppCompatActivity {

    private String userName = null;
    private String password = null;
    private String controlTopic = null;
    private String communicationTopic = null;

    private TextView peopleCount = null;
    private TextView adminsOnline = null;

    volatile Mqtt3AsyncClient client = null;
    volatile boolean connected = false;

    private final String TAG = this.getClass().getCanonicalName();
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metric);

        UserCredentials credentials = UserCredentials.load(this);
        if (credentials == null) {
            finishActivityWithError("Missing credentials fields", false);
            return;
        }

        userName = credentials.userName;
        password = credentials.password;
        controlTopic = credentials.controlTopic;
        communicationTopic = credentials.communicationTopic;

        peopleCount = findViewById(R.id.people_count);
        adminsOnline = findViewById(R.id.admins_online);

        client = MqttClient.builder().useMqttVersion3()
                                .identifier(UUID.randomUUID().toString())
                                .serverHost(getString(R.string.iot_cloud_host))
                                .serverPort(1883)
                                .buildAsync();

        client.connectWith()
                .simpleAuth()
                    .username(userName)
                    .password(password.getBytes())
                    .applySimpleAuth()
                .send()
                .whenComplete(this::connectionHandler);
    }

    private void connectionHandler(Mqtt3ConnAck connAck, Throwable throwable) {
        if (throwable != null) {
            throwable.printStackTrace();
            Log.e(TAG, "Error connecting to broker");
            finishActivityWithError("Error connecting to broker", false);
            return;
        }
        connected = true;

        Log.d(TAG, "Subscribing to metric topic: " + communicationTopic);
        client.subscribeWith()
                .topicFilter("channels/"+communicationTopic+"/messages")
                .callback(this::onMessageReceived)
                .send()
                .whenCompleteAsync(this::subscribed);
    }

    @Override
    protected void onDestroy() {
        if (client != null) {
            connected = false;
            client.disconnect();
            client = null;
        }
        super.onDestroy();
    }

    private void subscribed(Mqtt3SubAck connAck, Throwable throwable) {
        if (throwable != null) {
            throwable.printStackTrace();
            Log.e(TAG, "Error subscribing to topic: " + communicationTopic);
            finishActivityWithError("Error subscribing to topic: " + communicationTopic, false);
            return;
        }

        publishNext();
    }

    private void finishActivityWithError(String error, boolean clearCredentials) {
        if (clearCredentials) {
            UserCredentials.clear(this);
        }
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(getString(R.string.error_extra_name), error);
        startActivity(intent);
        finish();
    }

    private void finishActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void publishNext() {
        if (client == null || !connected) {
            Log.i(TAG, "Stoping publishing ping");
            return;
        }

        Log.d(TAG, "Publishing ping");

        client.publishWith()
                .topic("channels/"+controlTopic+"/messages")
                .payload("[{\"n\":\"ping\",\"v\":1}]".getBytes())
                .send()
                .whenCompleteAsync(((publish, throwable) -> {
                    if (throwable != null) {
                       throwable.printStackTrace();
                       finishActivityWithError("Error publishing to client!!! " + throwable.getMessage(), false);
                    } else {
                        try {
                            sleep(5000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        publishNext();
                    }
                }));

    }

    private void onMessageReceived(Mqtt3Publish publish) {
        if (!publish.getPayload().isPresent()) {
            Log.wtf(TAG, "Empty payload from server...");
            return;
        }
        ByteBuffer payload = publish.getPayload().get();
        byte[] arr = new byte[payload.remaining()];
        payload.get(arr);
        SenMLPacket[] packets = gson.fromJson(new String(arr), SenMLPacket[].class);
        if (packets == null) {
            Log.wtf(TAG, "Cannot parse payload: " + new String(arr));
            return;
        }

        for (SenMLPacket pack : packets) {
            if ("visitors".equals(pack.name)) {
                peopleCount.setText(String.valueOf(pack.value));
            }

            if ("active".equals(pack.name)) {
                adminsOnline.setText(String.valueOf(pack.value));
            }
        }
    }


    @Override
    public void onBackPressed() {
        finishActivity();
    }
}