package org.edu_sharing.restservices.about.v1.model;


import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
public class AboutFeatures {
    private final List<FeatureInfo> featureInfoList;

    public AboutFeatures(@Autowired(required = false) List<FeatureInfo> featureInfoList) {
        this.featureInfoList = featureInfoList;
    }
}
