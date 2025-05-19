package com.example.pebblethrower;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.Manifest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.example.pebblethrower.databinding.TypeNameBinding;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TypeName extends AppCompatActivity {
    private TypeNameBinding binding;
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        binding = TypeNameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    public void onRegister(View view) throws IOException {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
        }
        TextView tv = (TextView) findViewById(R.id.editTextText3);
        String name = tv.getText().toString();
        float max_velocity = getIntent().getFloatExtra("VELOCITY",0.0f);
        float distance = getIntent().getFloatExtra("DISTANCE",0.0f);
        boolean is_recorded = getIntent().getBooleanExtra("IS_RECORDED",false);
        Intent intent = new Intent(TypeName.this, Leaderboard.class);
        intent.putExtra("NAME",name);
        intent.putExtra("VELOCITY",max_velocity);
        intent.putExtra("DISTANCE",distance);
        Algorithm algorithm = Algorithm.HMAC256(getResources().getString(R.string.JWT_key));
        String jwt = JWT.create()
                .withIssuer("JWT")
                .withIssuedAt(new Date(System.currentTimeMillis()))
                .withExpiresAt(new Date(System.currentTimeMillis() + (3600 * 1000)))
                .withAudience("")
                .withSubject("")
                .withClaim("name",name)
                .withClaim("max_velocity",(double) max_velocity)
                .withClaim("distance",(double) distance)
                .sign(algorithm);
        HashMap<String,String> fluid = new HashMap<>();
        fluid.put("jwt",jwt);
        String myJSON = new Gson().toJson(fluid);
        Log.d("JWT",myJSON);
        insertThrow(getResources().getString(R.string.api_endpoint_setdata),myJSON);
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Background work here
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                // To update UI, use a Handler or runOnUiThread
            }
        }).start();
        if (is_recorded) {
            Log.d("File",String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM+"/Camera")));
            File dcimDir = new File(String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)));
            File cameraDir = new File(dcimDir, "Camera");
            Log.d("Hehe",cameraDir.getAbsolutePath());
            String path = getNewestFileInDirectory(cameraDir).getPath();
            Log.d("path",path);
            UploadFile(path);
        }
        setResult(RESULT_OK,intent);
        startActivity(intent);
        finish();
    }

    void UploadFile(String path) throws IOException{
        File file = new File(path);
        if (file.exists())
        {
            uploadFile(file, path);
        }
    }

    public void uploadFile(File fileToUpload, String path) {
        String[] paths = path.split("/");
        String filename = paths[paths.length - 1];
        OkHttpClient client = new OkHttpClient();

        // Create file request body
        RequestBody fileBody = RequestBody.create(fileToUpload, MediaType.parse("application/octet-stream"));

        // Build multipart form
        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", fileToUpload.getName(), fileBody)
                .build();

        // Build request
        Request request = new Request.Builder()
                .url(getResources().getString(R.string.upload_file_endpoint)+ filename + "/")
                .post(requestBody)
                .build();

        // Make async request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                // Notify UI thread
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String result = response.body().string();
                    Log.d("UPLOAD", "Success: " + result);
                } else {
                    Log.e("UPLOAD", "Failed: " + response.code());
                }
            }
        });
    }


    void insertThrow(String postURL, String POSTbody) throws IOException {
        OkHttpClient http = new OkHttpClient();
        RequestBody body = RequestBody.create(POSTbody,JSON);
        Request request = new Request.Builder().url(postURL).post(body).build();
        http.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                call.cancel();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("TAG",response.body().string());
            }
        });
    }

    private File getNewestFileInDirectory(File directory) {
        File[] files = directory.listFiles();

        if (files == null || files.length == 0) {
            return null;
        }

        File newestFile = files[0];
        for (File file : files) {
            if (file.lastModified() > newestFile.lastModified()) {
                newestFile = file;
            }
        }

        return newestFile;
    }
}
