package com.diemme.temperaturereader;

import android.util.Log;

public class Device {
   private String address;
   private int id;
   private String room;
   private String name;

   public Device()
   {};

   public Device (int id, String address, String name, String room){
       this.id=id;
       this.address=address;
       this.name=name;
       this.room=room;
   }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public void show(){
       Log.d("DeviceDB",id+ " "+ name+" "+ address+" "+room);
    }
}
