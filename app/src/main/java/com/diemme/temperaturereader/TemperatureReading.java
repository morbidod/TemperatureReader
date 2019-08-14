package com.diemme.temperaturereader;

/**
 * Created by XP011224 on 03/11/2017.
 */

public class TemperatureReading {
    private int temperature;
    private String address;
    private Long timestamp;
    private int rssi;



    private String room;

    public TemperatureReading(){};

    public TemperatureReading(int temperature, String address, Long timestamp, String room){
        this.temperature=temperature;
        this.address=address;
        this.timestamp=timestamp;
        this.room=room;
    }
    public TemperatureReading(int temperature, String address, Long timestamp, String room, int rssi){
        this.temperature=temperature;
        this.address=address;
        this.timestamp=timestamp;
        this.room=room;
        this.rssi=rssi;
    }

    public int getTemperature() {
        return temperature;
    }

    public String getAddress() {
        return address;
    }

    public String getRoom(){
        return room;
    }

    public Long getTimestamp() {
        return timestamp;
    }
    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public void setRoom(String room) {
        this.room = room;
    }
    public String getReadable(){
        return this.address +" " + this.room + " " + this.temperature;
    }
    public int getRssi(){
        int rssivalue=0;
        if(this.rssi>-150){
            rssivalue=this.rssi;
        }
        return rssivalue;
    }
}

