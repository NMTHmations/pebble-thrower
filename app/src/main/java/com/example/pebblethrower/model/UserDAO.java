package com.example.pebblethrower.model;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.pebblethrower.model.User;

import java.util.List;

@Dao
public interface UserDAO {
    @Query("SELECT * FROM USER")
    List<User> getAll();
    @Insert
    void Insert(User users);

    @Delete
    void DeleteUser(User users);
}