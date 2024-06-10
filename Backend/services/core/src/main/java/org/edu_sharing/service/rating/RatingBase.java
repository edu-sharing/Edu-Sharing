package org.edu_sharing.service.rating;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class RatingBase implements Serializable {

    private RatingData overall;
    private Map<String, RatingData> affiliation;


    public void setAffiliation(Map<String, RatingData> affiliation) {
        this.affiliation = affiliation;
    }

    public Map<String, RatingData> getAffiliation() {
        return affiliation;
    }

    public RatingData getOverall() {
        return overall;
    }

    public void setOverall(RatingData overall) {
        this.overall = overall;
    }

    @Schema
    public static class RatingData implements Serializable {
        private double sum;
        private long count;

        public RatingData() {}
        public RatingData(double sum, long count) {
            this.sum=sum;
            this.count=count;
        }

        public double getRating(){
            if(count==0)
                return 0;
            return (double)sum/count;
        }
        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public double getSum() {
            return sum;
        }

        public void setSum(double sum) {
            this.sum = sum;
        }
    }
}
