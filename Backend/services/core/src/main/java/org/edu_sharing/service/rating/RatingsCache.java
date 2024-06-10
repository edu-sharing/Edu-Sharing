package org.edu_sharing.service.rating;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Map;

@Schema
public class RatingsCache extends RatingBase implements Serializable {
    // ratings per user/authority
    private Map<String, Double> users;


    public <R> void setUsers(Map<String, Double> users) {
        this.users = users;
    }

    public Map<String, Double> getUsers() {
        return users;
    }
}
