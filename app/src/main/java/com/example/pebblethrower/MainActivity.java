package com.example.pebblethrower;

import static java.util.Collections.max;

import androidx.annotation.NonNull;
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
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.pebblethrower.databinding.ActivityMainBinding;

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

    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private MediaRecorder mediaRecorder;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private Size videoSize;
    private Surface recorderSurface;
    private boolean isRecording = false;
    private TextureView textureView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        textureView = findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(textureListener);
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, 200);
            return;  // Wait for user permission result
        }
        startBackgroundThread();
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

        Button button = (Button) view;

        if (button.getText().equals("Start")) {
            startSensorTracking();
            startBackgroundThread();
            if (textureView.isAvailable()) {
                Log.d("MediaRecorder","MediaRecorder called");
                openCamera();
            } else {
                Log.d("MediaRecorder","Set texture Listener called");
                textureView.setSurfaceTextureListener(textureListener);
            }
            start = System.currentTimeMillis();
            button.setText("Stop");
        } else {
            stopSensorTracking();
            stopRecordingVideo();
            closeCamera();
            stopBackgroundThread();

            button.setText("Start");

            float sum = 0;
            for (float v : velocity_list) {
                sum += v;
            }
            velocity = sum / velocity_list.size();

            Intent intent = new Intent(MainActivity.this, TypeName.class);
            intent.putExtra("VELOCITY", velocity);
            intent.putExtra("DISTANCE", distance);
            setResult(RESULT_OK, intent);
            startActivity(intent);
            finish();
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

    private final TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.d("Camera2", "SurfaceTexture available");
            openCamera();
        }
        @Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}
        @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) { return false; }
        @Override public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
    };

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            videoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));

            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                manager.openCamera(cameraId, stateCallback, backgroundHandler);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == 1920 && size.getHeight() == 1080) {
                return size;
            }
        }
        return choices[0];
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            startPreview();
        }
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            cameraDevice = null;
        }
        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            cameraDevice = null;
        }
    };

    private void startPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            texture.setDefaultBufferSize(videoSize.getWidth(), videoSize.getHeight());
            Surface previewSurface = new Surface(texture);

            setupMediaRecorder();

            recorderSurface = mediaRecorder.getSurface();

            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            builder.addTarget(previewSurface);
            builder.addTarget(recorderSurface);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cameraDevice.createCaptureSession(List.of(previewSurface, recorderSurface), new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        captureSession = session;
                        try {
                            captureSession.setRepeatingRequest(builder.build(), null, backgroundHandler);
                            startRecordingVideo();  // Start recording as soon as preview is ready
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        Toast.makeText(MainActivity.this, "Camera configuration failed", Toast.LENGTH_SHORT).show();
                    }
                }, backgroundHandler);
            }
        } catch (CameraAccessException | IOException e) {
            e.printStackTrace();
        }
    }

    private void setupMediaRecorder() throws IOException {
        if (mediaRecorder == null) {
            mediaRecorder = new MediaRecorder();
        } else {
            mediaRecorder.reset();
        }
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        String outputFile = getExternalFilesDir(null) + "/video_" + System.currentTimeMillis() + ".mp4";
        Log.d("MediaRecorder","Output file: "+outputFile);
        mediaRecorder.setOutputFile(outputFile);

        mediaRecorder.setVideoEncodingBitRate(10000000);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setVideoSize(videoSize.getWidth(), videoSize.getHeight());
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        Log.d("MediaRecorder", "Preparing MediaRecorder...");
        mediaRecorder.prepare();
        Log.d("MediaRecorder", "MediaRecorder prepared.");
    }

    private void startRecordingVideo() {
        if (cameraDevice == null || isRecording) return;
        try {
            mediaRecorder.start();
            isRecording = true;
            Log.d("MediaRecorder", "Recording started.");
            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
        } catch (IllegalStateException e) {
            Log.e("MediaRecorder", "start() failed", e);
            e.printStackTrace();
        }
    }

    private void stopRecordingVideo() {
        if (!isRecording) return;
        try {
            mediaRecorder.stop();
            mediaRecorder.reset();
            Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        isRecording = false;
    }

    private void closeCamera() {
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 200) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

}