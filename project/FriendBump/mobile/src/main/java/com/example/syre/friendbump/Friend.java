package com.example.syre.friendbump;

/**
 * Created by Anders on 08-04-2015.
 */
public class Friend
{
    private String name;
    private String email;
    private double lat;
    private double lng;
    private boolean notification;
    public Friend(String name)
    {
        this.name = name;
    }

    public Friend(String name, double lat, double lng, String email)
    {
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.email = email;
        this.notification = false;
    }

    public String getName()
    {
        return this.name;
    }
    public String getEmail() { return email; }
    public double getLat()
    {
        return this.lat;
    }
    public double getLng()
    {
        return this.lng;
    }
    public boolean getNotification() {return  this.notification;}

    public void setLat(double lat)
    {
        this.lat = lat;
    }
    public void setLng(double lng)
    {
        this.lng = lng;
    }
    public void setEmail(String email) {this.email = email;}
    public void setNotification(boolean flag) {this.notification = flag;}

    @Override
    public String toString()
    {
        return this.name;
    }
}
