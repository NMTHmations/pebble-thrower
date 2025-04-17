package com.example.pebblethrower;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.example.pebblethrower.databinding.LeaderboardBinding;
import com.example.pebblethrower.model.AppDatabase;
import com.example.pebblethrower.model.User;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Leaderboard extends AppCompatActivity {
    StringBuilder result = new StringBuilder();

    ListView item;

    class ListItem {
        String title;
        String subitem;
        String id;
        ListItem(String title, String subitem, String id) {
            this.title = title;
            this.subitem = subitem;
            this.id = id;
        }
    }

    class MyAdapter extends ArrayAdapter<ListItem> {
        public MyAdapter(Context context, List<ListItem> items) {
            super(context, 0, items);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            ListItem item = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.inlinelayout, parent, false);
            }

            TextView title = convertView.findViewById(R.id.titleText);
            TextView sub = convertView.findViewById(R.id.subItemText);

            title.setText(item.title);
            sub.setText(item.subitem);

            return convertView;
        }
    }

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
        SetAdapterOffline();
    }

    public void SetAdapterOffline()
    {
        AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "database-name").allowMainThreadQueries().build(); // not recommended, but only works in this way
        List<User> userList = db.userDao().getAll();
        int number = userList.size() + 1;
        User user = createUser(number);
        db.userDao().Insert(user);
        userList = db.userDao().getAll();
        List<ListItem> items = new ArrayList<>();
        item = (ListView) findViewById(R.id.listview);
        for (var i : userList)
        {
            items.add(new ListItem(i.getName(),"Speed:"+i.getMax_velocity()+"","dont-click"));
        }
        MyAdapter adapter = new MyAdapter(this, items);
        item.setAdapter(adapter);
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
            SetAdapterOffline();
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
                        String responseData = response.body().string();
                        List<ListItem> items = new ArrayList<>();
                        item = (ListView) findViewById(R.id.listview);
                        try {
                                JSONArray wrapped = new JSONArray("[" + responseData + "]");
                                String innerJsonString = wrapped.getString(0);

                                // Then parse that string into a real JSONArray
                                JSONArray arrayJson = new JSONArray(innerJsonString);
                                if (arrayJson.length() == 0) {
                                    items.add(new ListItem("List is currently empty!","Be the first to publish it","dont-click"));
                                }
                                for (int i = 0; i < arrayJson.length(); i++) {
                                    items.add(new ListItem(arrayJson.getJSONObject(i).getString("name"),"Speed:"+arrayJson.getJSONObject(i).getString("max_velocity")+", Distance: "+arrayJson.getJSONObject(i).getString("distance"),arrayJson.getJSONObject(i).getString("id")));
                                }
                        } catch (JSONException e) {
                            Log.d("ERROR",e.getMessage());
                            items.add(new ListItem("Error happened!","We are working on the solution!","dont-click"));
                        }
                        Leaderboard.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                MyAdapter adapter = new MyAdapter(Leaderboard.this, items);
                                item.setAdapter(adapter);
                                item.setOnItemClickListener((parent, view1, position, id) -> {
                                    ListItem selectedItem = (ListItem) parent.getItemAtPosition(position);
                                    Log.d("POS",selectedItem.id);
                                    if (!selectedItem.id.equals("dont-click"))
                                    {
                                        openChrome(view,selectedItem.id);
                                    }
                                });
                            }
                        });
                    }
                }
            });
        }
    }
    public void openChrome(View view, String id) {
        String url = "http://178.238.212.244/data/" + id; // Replace with your desired URL
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(android.net.Uri.parse(url));
        intent.setPackage("com.android.chrome"); // This ensures it opens in Chrome

        // Fallback if Chrome is not installed
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            // Open with default browser
            Intent fallback = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url));
            startActivity(fallback);
        }
    }
}
