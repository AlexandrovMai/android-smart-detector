package ru.mai.smartdetect;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class MainActivity extends AppCompatActivity {

    private EditText userName = null;
    private EditText password = null;
    private EditText communicationTopic = null;
    private EditText controlTopic = null;
    private Button qrCodeButton = null;
    private Button signInButton = null;

    private static final int PERMISSION_REQUEST_CAMERA = 0;
    private final String TAG = this.getClass().getCanonicalName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userName = findViewById(R.id.username);
        password = findViewById(R.id.mqtt_psw);
        communicationTopic = findViewById(R.id.com_topic);
        controlTopic = findViewById(R.id.con_topic);

        qrCodeButton = findViewById(R.id.qr_read);
        signInButton = findViewById(R.id.login);

        loadPreferences();
        qrCodeButton.setOnClickListener(view -> requestCamera());
        signInButton.setOnClickListener(view -> signIn());

        Bundle extra = getIntent().getExtras();
        if (extra == null) {
            return;
        }

        if (extra.getString(getString(R.string.error_extra_name)) != null) {
            showErrorDialog(extra.getString(getString(R.string.error_extra_name)));
        }
    }

    private void requestCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CAMERA);
        }
    }

    private boolean loadPreferences() {
        UserCredentials credentials = UserCredentials.load(this);
        if (credentials == null) {
            return false;
        }

        userName.setText(credentials.userName);
        password.setText(credentials.password);
        communicationTopic.setText(credentials.communicationTopic);
        controlTopic.setText(credentials.controlTopic);
        return true;
    }

    private void signIn() {

        UserCredentials.clear(this);
        new UserCredentials (
                userName.getText().toString(),
                password.getText().toString(),
                controlTopic.getText().toString(),
                communicationTopic.getText().toString()
        ).save(this);

        Intent intent = new Intent(this, MetricActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                showErrorDialog("Camera Permission Denied");
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() != null) {
                parseQRPayload(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    private void showErrorDialog(String message) {
        ErrorDialogFragment myDialogFragment =  new ErrorDialogFragment(message);
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        myDialogFragment.show(transaction, "dialog");
    }

    private void startCamera() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(false);
        integrator.setCaptureActivity(CaptureActivityPortrait.class);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
        integrator.initiateScan();
    }

    private void parseQRPayloadUnsafe(String payload) {
        String[] messages = payload.split(":");
        if (messages.length != 4)
            throw new IllegalArgumentException("QR Code data malformed");
        userName.setText(messages[0]);
        password.setText(messages[1]);
        controlTopic.setText(messages[2]);
        communicationTopic.setText(messages[3]);
    }

    private void parseQRPayload(String payload) {
        try {
            parseQRPayloadUnsafe(payload);
        } catch (Exception e) {
            e.printStackTrace();
            showErrorDialog("Something Went wrong\n" + e.getMessage());
        }
    }

}