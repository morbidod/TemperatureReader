package com.diemme.temperaturereader;

/**
 * Created by XP011224 on 09/11/2017.
 */

public class User {
    private String name;
    private String uid;
    private Boolean isWriter;

    public User() {
    }

    public User(String name, String uid,Boolean isWriter ) {

        this.name = name;
        this.uid = uid;
        this.isWriter = isWriter;
    }

    public Boolean getIsWriter() {
        return isWriter;
    }

    public void setIsWriter(Boolean isWriter) {
        this.isWriter = isWriter;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
