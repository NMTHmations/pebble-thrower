package com.example.pebblethrower;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pebblethrower.databinding.ActivityMainBinding;
import com.example.pebblethrower.databinding.TypeNameBinding;

public class TypeName extends AppCompatActivity {
    private TypeNameBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        binding = TypeNameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    public void onRegister(View view) {
        TextView tv = (TextView) findViewById(R.id.editTextText3);
        String name = tv.getText().toString();
        float max_velocity = getIntent().getFloatExtra("VELOCITY",0.0f);
        Intent intent = new Intent(TypeName.this, Leaderboard.class);
        intent.putExtra("NAME",name);
        intent.putExtra("VELOCITY",max_velocity);
        setResult(RESULT_OK,intent);
        startActivity(intent);
        finish();
    }
}
