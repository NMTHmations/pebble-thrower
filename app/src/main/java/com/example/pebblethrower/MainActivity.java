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
    public SensorManager sensorManager;
    public Sensor accelometer;
    public SensorEventListener accEventListener;
    public boolean FirstAttempt = true;
    float init_acc = 0;
    public float avg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Example of a call to a native method
        TextView tv = binding.sampleText.findViewById(R.id.sample_text);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accEventListener = new SensorEventListener() {

            @Override
            public void onSensorChanged(SensorEvent event) {
                if (FirstAttempt)
                {
                    init_acc = event.values[1];
                    FirstAttempt = false;
                }
                List<Float> sensor_data = new ArrayList<Float>();
                sensor_data.add(Math.abs(event.values[0]));
                sensor_data.add(Math.abs(event.values[1] - init_acc));
                sensor_data.add(Math.abs(event.values[2]));
                avg = max(sensor_data);
                tv.setText(""+avg);
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
                button.setText("Stop");
            }
            else
            {
                onResume();
                button.setText("Start");
                Intent intent = new Intent(MainActivity.this, TypeName.class);
                intent.putExtra("VELOCITY",avg);
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
    }

    @Override
    public void onPause()
    {
        super.onPause();
        sensorManager.registerListener(accEventListener,accelometer, SensorManager.SENSOR_DELAY_NORMAL);
    }
}