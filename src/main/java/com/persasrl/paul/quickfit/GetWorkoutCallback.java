package com.persasrl.paul.quickfit;

/**
 * Created by Paul on 9/6/2015.
 */
public interface GetWorkoutCallback {
    /**
     * Invoked when background task is completed
     */

    public abstract void done(Workout returnedWorkout);
}
