/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.serverlist.entity;


import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@SuppressWarnings("unused")
@Entity(tableName = "ConfigFile")
public class ConfigFile {

    @PrimaryKey
    @ColumnInfo(name = "primary_key")
    public int primaryKey;

    @ColumnInfo(name = "Content")
    private String content;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "password")
    private String password;

    @ColumnInfo(name = "remember")
    private boolean remember;

    @Ignore
    private int type = 1;

    @ColumnInfo(name = "username")
    private String username;

    public ConfigFile(int primaryKey, String name, String content, String username, String password,
            boolean remember) {
        this.primaryKey = primaryKey;
        this.content = content;
        this.username = username;
        this.password = password;
        this.name = name;
        this.remember = remember;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(int primaryKey) {
        this.primaryKey = primaryKey;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isRemember() {
        return remember;
    }

    public void setRemember(boolean remember) {
        this.remember = remember;
    }

    @NonNull
    @Override
    public String toString() {
        return "ConfigFile{" +
                "primaryKey=" + primaryKey +
                ", content='" + content + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", remember=" + remember +
                ", type=" + type +
                ", name='" + name + '\'' +
                '}';
    }

}
