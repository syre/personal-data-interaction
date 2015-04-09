package com.example.syre.friendbump;

/**
 * Created by Anders on 08-04-2015.
 */
public class friend
{
    private String name;
    private double lat;
    private double lng;

    public friend(String name)
    {
        this.name = name;
    }

    public friend(String name, double lat, double lng)
    {
        this.name = name;
        this.lat = lat;
        this.lng = lng;
    }

    String getName()
    {
        return this.name;
    }

    double getLat()
    {
        return this.lat;
    }

    double getLng()
    {
        return this.lng;
    }

    @Override
    public String toString()
    {
        return this.name;
    }
}
