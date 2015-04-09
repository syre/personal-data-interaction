package com.example.syre.friendbump;

/**
 * Created by Anders on 08-04-2015.
 */
public class Friend
{
    private String name;
    private double lat;
    private double lng;

    public Friend(String name)
    {
        this.name = name;
    }

    public Friend(String name, double lat, double lng)
    {
        this.name = name;
        this.lat = lat;
        this.lng = lng;
    }

    public String getName()
    {
        return this.name;
    }

    public double getLat()
    {
        return this.lat;
    }

    public double getLng()
    {
        return this.lng;
    }

    public void setLat(double lat)
    {
        this.lat = lat;
    }
    public void setLng(double lng)
    {
        this.lng = lng;
    }

    @Override
    public String toString()
    {
        return this.name;
    }
}
