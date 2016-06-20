package com.persasrl.paul.quickfit;

/**
 * Created by Paul on 9/6/2015.
 */
public class Workout {
    float dist;
    long time;
    String user;
    int type;
    public Workout(float dist, int type, String user, long time )
    {
        this.dist = dist;
        this.type = type;
        this.time = time;
        this.user = user;

    }
}
