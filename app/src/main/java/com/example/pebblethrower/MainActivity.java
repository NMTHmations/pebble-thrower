package com.example.pebblethrower;

import static java.util.Collections.max;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.example.pebblethrower.databinding.ActivityMainBinding;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;
import android.widget.Toast;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 100;

    // Used to load the 'pebblethrower' library on application startup.
    static {
        System.loadLibrary("pebblethrower");
    }

    private ActivityMainBinding binding;
    private SensorManager sensorManager;
    private Sensor accelometer;
    private SensorEventListener accEventListener;
    private boolean FirstAttempt = true;
    private float init_acc = 0;
    private float maximum;
    private List<Float> velocity_list = new ArrayList<Float>();
    public float velocity;
    private long start;

    private float distance;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, 200);
            return;  // Wait for user permission result
        }
        TextView tv = binding.sampleText.findViewById(R.id.sample_text);
        TextView dist = binding.sampleText2.findViewById(R.id.sample_text2);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accEventListener = new SensorEventListener() {

            @Override
            public void onSensorChanged(SensorEvent event) {
                if (FirstAttempt)
                {
                    init_acc = event.values[1];
                    FirstAttempt = false;
                    distance = 0;
                }
                List<Float> sensor_data = new ArrayList<Float>();
                sensor_data.add(Math.abs(event.values[0]));
                sensor_data.add(Math.abs(event.values[1] - init_acc));
                sensor_data.add(Math.abs(event.values[2]));
                maximum = max(sensor_data);
                float finish = (float) (start - System.currentTimeMillis()) / (float) 1000;
                float moment = (float) 1 /2 * maximum * finish * finish;
                velocity_list.add(moment);
                distance += moment * finish;
                tv.setText(""+moment);
                dist.setText(Float.toString(distance));
                start = System.currentTimeMillis();
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
    }

    /**
     * A native method that is implemented by the 'pebblethrower' native library,
     * which is packaged with this application.
     */

    public void ChangeText(View view) throws IOException {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.RECORD_AUDIO}, CAMERA_REQUEST_CODE);
            return; // Wait for permissions before continuing
        }
        CheckBox simpleCheckBox = (CheckBox) findViewById(R.id.checkBox2);
        Button button = (Button) view;

        if (button.getText().equals("Start")) {
            startSensorTracking();
            if (simpleCheckBox.isChecked()) {
                Intent i = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                startActivityForResult(i, 1);
            }
            start = System.currentTimeMillis();
            button.setText("Stop");
        } else {
            stopSensorTracking();

            button.setText("Start");

            float sum = 0;
            for (float v : velocity_list) {
                sum += v;
            }
            velocity = sum / velocity_list.size();

            Intent intent = new Intent(MainActivity.this, TypeName.class);
            intent.putExtra("VELOCITY", velocity);
            intent.putExtra("DISTANCE", distance);
            if (simpleCheckBox.isChecked()) {
                intent.putExtra("IS_RECORDED", true);
            }
            else
            {
                intent.putExtra("IS_RECORDED", false);
            }
            setResult(RESULT_OK, intent);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == 1) {
            Log.d("Video","Video done!");
        }
    }

    private void startSensorTracking() {
        sensorManager.registerListener(accEventListener, accelometer, SensorManager.SENSOR_DELAY_NORMAL);
        FirstAttempt = true;
        start = System.currentTimeMillis();
    }

    private void stopSensorTracking() {
        sensorManager.unregisterListener(accEventListener);
    }

}