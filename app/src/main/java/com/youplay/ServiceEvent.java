package com.youplay;

/**
 * Created by tan on 20/05/17.
 **/

public class ServiceEvent {
    private final String event;
    private final int timerValue;

    public ServiceEvent(String event) {
        this.event = event;
        this.timerValue = 0;
    }

    public ServiceEvent(String event, int timerValue) {
        this.event = event;
        this.timerValue = timerValue;
    }

    public String getEvent() {
        return event;
    }

    public int getTimerValue() {
        return timerValue;
    }
}
