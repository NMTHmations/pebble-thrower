package com.example.pebblethrower;

import static java.util.Collections.max;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.pebblethrower.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

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
        // Example of a call to a native method
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

    public void ChangeText(View view){
        var button = (Button) view;
        TextView tv = view.findViewById(R.id.sample_text);
        if (button.isPressed())
        {
            if (button.getText().equals("Start")) {
                onPause();
                start = System.currentTimeMillis();
                button.setText("Stop");
            }
            else
            {
                onResume();
                button.setText("Start");
                float sum = 0;
                for (var i : velocity_list)
                {
                    sum += i;
                }
                velocity = sum / velocity_list.size();
                Intent intent = new Intent(MainActivity.this, TypeName.class);
                intent.putExtra("VELOCITY",velocity);
                setResult(RESULT_OK,intent);
                startActivity(intent);
                finish();
            }
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        sensorManager.unregisterListener(accEventListener);
        FirstAttempt = true;
    }

    @Override
    public void onPause()
    {
        super.onPause();
        sensorManager.registerListener(accEventListener,accelometer, SensorManager.SENSOR_DELAY_NORMAL);
    }
}