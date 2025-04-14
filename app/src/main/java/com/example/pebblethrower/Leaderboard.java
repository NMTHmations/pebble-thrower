package com.example.pebblethrower;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.example.pebblethrower.databinding.LeaderboardBinding;
import com.example.pebblethrower.model.AppDatabase;
import com.example.pebblethrower.model.User;

import java.util.List;

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

    public void switched(View view)
    {
        //User user = new User(getIntent().getStringExtra("NAME"),getIntent().getFloatExtra("VELOCITY",0.0f));
        Switch switch_controller = (Switch) findViewById(R.id.switch1);
        if (!switch_controller.isChecked()) {
            TextView tv = findViewById(R.id.textView4);
            tv.setText(result);
        }
        else
        {
            TextView tv = findViewById(R.id.textView4);
            tv.setText("The online leaderboard will show up here");
        }
    }
}
