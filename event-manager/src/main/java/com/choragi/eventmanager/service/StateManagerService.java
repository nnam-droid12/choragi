package com.choragi.eventmanager.service;

import com.choragi.eventmanager.model.GlobalTourState;
import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StateManagerService {

    private final ConcurrentHashMap<String, GlobalTourState> activeTours = new ConcurrentHashMap<>();

    public void updateState(GlobalTourState state) {
        activeTours.put(state.getTourId(), state);
    }

    public GlobalTourState getState(String tourId) {
        return activeTours.get(tourId);
    }

    public void transitionTo(String tourId, String nextStatus) {
        GlobalTourState state = activeTours.get(tourId);
        if (state != null) {
            state.setStatus(nextStatus);
            state.log("Transitioning workflow to: " + nextStatus);
            updateState(state);
        }
    }
}