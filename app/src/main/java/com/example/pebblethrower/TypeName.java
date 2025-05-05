package com.example.pebblethrower;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.example.pebblethrower.databinding.TypeNameBinding;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
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
        TextView tv = (TextView) findViewById(R.id.editTextText3);
        String name = tv.getText().toString();
        float max_velocity = getIntent().getFloatExtra("VELOCITY",0.0f);
        float distance = getIntent().getFloatExtra("DISTANCE",0.0f);
        String path = getIntent().getStringExtra("FILENAME_PATH");
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
        UploadFile(path);
        setResult(RESULT_OK,intent);
        startActivity(intent);
        finish();
    }

    void UploadFile(String path) throws IOException{
        File file = new File(this.getFilesDir(), path);
        if (file.exists())
        {
            uploadFile(file, path);
        }
        else
        {
            file = new File(Environment.getExternalStorageDirectory(),path);
            if (file.exists())
            {
                uploadFile(file, path);
            }
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
}
