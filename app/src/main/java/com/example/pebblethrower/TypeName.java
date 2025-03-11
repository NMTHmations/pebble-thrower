package com.example.pebblethrower;

import android.os.Bundle;

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
}
