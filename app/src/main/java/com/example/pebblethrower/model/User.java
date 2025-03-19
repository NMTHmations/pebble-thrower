package com.example.pebblethrower.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
@Entity
public class User {
    @PrimaryKey
    private int uint;
    @ColumnInfo(name = "name")
    private String name;
    @ColumnInfo(name = "max_velocity")
    private float max_velocity;

    public void setUint(int uint){
        this.uint = uint;
    }

    public int getUint(){
        return uint;
    }

    public String getName(){
        return name;
    }

    public float getMax_velocity(){
        return max_velocity;
    }

    public void setName(String name){
        this.name = name;
    }

    public void setMax_velocity(float max_velocity){
        this.max_velocity = max_velocity;
    }
}
