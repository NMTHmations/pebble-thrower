package com.example.pebblethrower;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.example.pebblethrower.databinding.LeaderboardBinding;
import com.example.pebblethrower.model.AppDatabase;
import com.example.pebblethrower.model.User;
import com.example.pebblethrower.model.UserDAO;

import java.util.ArrayList;
import java.util.List;

public class Leaderboard extends AppCompatActivity {
    private LeaderboardBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        binding = LeaderboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "database-name").allowMainThreadQueries().build(); // not recommended, but only works in this way
        //User user = new User(getIntent().getStringExtra("NAME"),getIntent().getFloatExtra("VELOCITY",0.0f));
        User user = new User();
        user.setName(getIntent().getStringExtra("NAME"));
        user.setVelocity(getIntent().getFloatExtra("VELOCITY",0.0f));
        db.userDao().Insert(user);
        List<User> userList =  db.userDao().getAll();
        StringBuilder result = new StringBuilder();
        for (var i : userList)
        {
            result.append("Name: "+i.name+", Velocity: "+i.max_velocity);
        }
        TextView tv = findViewById(R.id.textView4);
        tv.setText(result);
    }
}
