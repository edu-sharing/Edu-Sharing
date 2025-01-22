package org.edu_sharing.restservices.tracking.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.edu_sharing.restservices.shared.Group;
import org.edu_sharing.restservices.shared.Organization;
import org.edu_sharing.service.tracking.TrackingService;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class Tracking {
    private Map<TrackingService.EventType, Integer> counts;
    private String date;
    @JsonProperty("authority")
    private TrackingAuthority trackingAuthority;
    private Map<String, Serializable> fields;
    private Map<TrackingService.EventType, Map<String, Map<String, Long>>> groups;

    public Tracking(){}
    public Tracking(String date, TrackingAuthority trackingAuthority, Map<TrackingService.EventType,Integer> counts, Map<String,Serializable> fields, Map<TrackingService.EventType, Map<String, Map<String, Long>>> groups){
        this.date=date;
        this.trackingAuthority = trackingAuthority;
        this.counts=counts;
        this.fields=fields;
        this.groups=groups;

    }

    @Data
    public static class TrackingAuthority {
        private String hash;
        private List<Organization> organization;
        private List<Group> mediacenter;
    }
}
