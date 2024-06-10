/*
 * Public API Surface of ngx-edu-sharing-api
 */

export {
    EduSharingApiConfigurationParams,
    EDU_SHARING_API_CONFIG,
} from './lib/edu-sharing-api-configuration';
export { ApiRequestConfiguration } from './lib/api-request-configuration';
export * from './lib/constants';
export * from './lib/edu-sharing-api.module';
export * from './lib/models';
export * from './lib/model-overrides/proposals';
export * from './lib/wrappers/about.service';
export * from './lib/wrappers/api-helpers.service';
export * from './lib/wrappers/authentication.service';
export * from './lib/wrappers/collection.service';
export * from './lib/wrappers/config.service';
export * from './lib/wrappers/connector.service';
export * from './lib/wrappers/lti-platform.service';
export * from './lib/wrappers/mediacenter.service';
export * from './lib/wrappers/mds-label.service';
export * from './lib/wrappers/mds.service';
export * from './lib/wrappers/network.service';
export * from './lib/wrappers/node.service';
export * from './lib/wrappers/nodeList.service';
export * from './lib/wrappers/register.service';
export * from './lib/wrappers/relation.service';
export * from './lib/wrappers/saved-searches.service';
export * from './lib/wrappers/search.service';
export * from './lib/wrappers/session-storage.service';
export * from './lib/wrappers/user.service';
export * from './lib/rest-constants';

// Unwrapped API services.
//
// Exporting services here indicates that these services are safe to use and don't interfere with
// any wrappers. If you make assumptions in wrappers about what API calls have been made, make sure
// to not expose these API calls here, but instead provide a wrapper that keeps track of calls.
export {
    MdsV1Service,
    StreamV1Service,
    ClientutilsV1Service,
    TrackingV1Service,
    NotificationV1Service,
    FeedbackV1Service,
    SuggestionsV1Service,
    LtiPlatformV13Service,
    RatingV1Service,
} from './lib/api/services';
