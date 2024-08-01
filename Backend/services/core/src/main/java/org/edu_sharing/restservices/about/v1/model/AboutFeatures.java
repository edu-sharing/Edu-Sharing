package org.edu_sharing.restservices.about.v1.model;


import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Getter
public class AboutFeatures {
    @Autowired(required = false)
    List<FeatureInfo> featureInfoList;
}
