package org.edu_sharing.service.handleservice;

import lombok.RequiredArgsConstructor;
import org.apache.log4j.Logger;
import org.edu_sharing.service.handleservicedoi.DOIService;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HandleServiceFactory {
    public enum IMPLEMENTATION {
        /**
         * handle.net service implementation
         */
        handle,
        /**
         * doi datacite implementation
         */
        doi
    }
    private final HandleServiceImpl handleService;
    private final DOIService doiService;

    public HandleService instance(IMPLEMENTATION type) throws HandleServiceNotConfiguredException{
        if(type == IMPLEMENTATION.doi){
            return doiService;
        } else {
            return handleService;
        }
    }
}
