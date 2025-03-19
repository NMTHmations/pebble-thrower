package com.example.pebblethrower.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class User {
    @PrimaryKey
    public int uint;
    @ColumnInfo(name = "name")
    public String name;
    @ColumnInfo(name = "max_velocity")
    public float max_velocity;
}
