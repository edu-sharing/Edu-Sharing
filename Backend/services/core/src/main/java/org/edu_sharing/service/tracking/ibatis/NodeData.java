package org.edu_sharing.service.tracking.ibatis;

import org.edu_sharing.service.tracking.TrackingService;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class NodeData {
    private String timestamp;
    Map<TrackingService.EventType, Integer> counts = new HashMap<>();

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Map<TrackingService.EventType, Integer> getCounts() {
        return counts;
    }

    public void setCounts(String countsJson) {
        try {
            JSONObject object = new JSONObject(countsJson);
            for (Iterator it = object.keys(); it.hasNext(); ) {
                String key = (String) it.next();
                counts.put(TrackingService.EventType.valueOf(key), object.getInt(key));
            }
        } catch (JSONException ignored) {}

    }
}
