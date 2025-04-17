package com.example.pebblethrower;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.example.pebblethrower.databinding.LeaderboardBinding;
import com.example.pebblethrower.model.AppDatabase;
import com.example.pebblethrower.model.User;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Leaderboard extends AppCompatActivity {
    StringBuilder result = new StringBuilder();

    public User createUser(int uint){
        User user = new User();
        user.setUint(uint);
        user.setName(getIntent().getStringExtra("NAME"));
        user.setMax_velocity(getIntent().getFloatExtra("VELOCITY",0.0f));
        return user;
    }
    private LeaderboardBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        binding = LeaderboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "database-name").allowMainThreadQueries().build(); // not recommended, but only works in this way
        List<User> userList = db.userDao().getAll();
        int number = userList.size() + 1;
        User user = createUser(number);
        db.userDao().Insert(user);
        userList = db.userDao().getAll();
        for (var i : userList) {
            result.append("Name: " + i.getName() + ", Velocity: " + i.getMax_velocity() + "\n");
        }
        TextView tv = findViewById(R.id.textView4);
        tv.setText(result);
    }

    public void onExit(View view)
    {
        finishAffinity();
    }

    public void onNew(View view)
    {
        Intent intent = new Intent(Leaderboard.this,MainActivity.class);
        startActivity(intent);
        finish();
    }

    public void switched(View view) throws IOException {
        //User user = new User(getIntent().getStringExtra("NAME"),getIntent().getFloatExtra("VELOCITY",0.0f));
        Switch switch_controller = (Switch) findViewById(R.id.switch1);
        TextView tv = findViewById(R.id.textView4);
        if (!switch_controller.isChecked()) {
            tv.setText(result);
        }
        else
        {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(getResources().getString(R.string.api_endpoint_getlist))
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        StringBuilder string = new StringBuilder();
                        String responseData = response.body().string();
                        try {
                                JSONArray wrapped = new JSONArray("[" + responseData + "]");
                                String innerJsonString = wrapped.getString(0);

                                // Then parse that string into a real JSONArray
                                JSONArray arrayJson = new JSONArray(innerJsonString);
                                if (arrayJson.length() == 0) {
                                    string.append("The results from the online leaderboard will show up here\n");
                                }
                                for (int i = 0; i < arrayJson.length(); i++) {
                                    string.append("Data: " + arrayJson.getJSONObject(i).getString("name")+ "\n");
                                }
                        } catch (JSONException e) {
                            Log.d("ERROR",e.getMessage());
                            string.append("Error happened, when we tried to fetch the data! Check back later.\n");
                        }
                        Leaderboard.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tv.setText(string.toString());
                            }
                        });
                    }
                }
            });
        }
    }
}
