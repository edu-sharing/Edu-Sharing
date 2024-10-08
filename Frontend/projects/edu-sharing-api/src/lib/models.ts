// Reexport API models that are exposed by wrappers.

import { SearchV1Service } from './api/services/search-v-1.service';

export {
    About,
    Connector,
    ConnectorFileType,
    ConnectorList,
    CollectionReference,
    FeedbackData,
    Acl,
    Ace,
    Authority,
    LicenseAgreement,
    ManualRegistrationData,
    HandleParam,
    NodePermissions as NodePermissionsGet,
    Mds as MdsDefinition,
    MdsGroup,
    MdsQueryCriteria,
    MdsSort,
    MdsSortColumn,
    MdsSortDefault,
    MdsValue,
    MdsView,
    FeatureInfo,
    MdsWidget,
    MdsWidgetCondition,
    MetadataSetInfo,
    NotificationEventDto as Notification,
    ConfigTutorial,
    Mediacenter,
    NotificationConfig,
    Node,
    NodeEntries,
    NodeVersion,
    NodeVersionEntries,
    NodeVersionRef,
    NodeVersionRefEntries,
    NodeRef,
    NodeStats,
    Config,
    ConfigThemeColor,
    Organization,
    Group,
    GroupProfile,
    UserProfileEdit,
    MediacenterProfileExtension,
    Pagination,
    Person,
    RelationData,
    ReferenceEntries,
    SearchResultNode as SearchResults,
    SearchParameters,
    Repo as Repository,
    Statistics,
    StatisticsGroup,
    StreamEntry,
    Tool,
    NodeSuggestionResponseDto,
    SuggestionResponseDto,
    Tools,
    User,
    UserProfile,
    UserQuota,
    UserStatus,
    WebsiteInformation,
    RegisterInformation,
} from './api/models';
import { HttpErrorResponse } from '@angular/common/http';
import { Acl, Group, MdsView, Organization, Person, User } from './api/models';
import { SuggestionsV1Service } from './api/services/suggestions-v-1.service';

export type SuggestionStatus = Parameters<SuggestionsV1Service['updateStatus']>[0]['status'];
export type MdsViewRelation = MdsView['rel'];
export type GenericAuthority = Organization | Group | User;
export type ApiErrorResponse = HttpErrorResponse & {
    readonly defaultPrevented: boolean;
    preventDefault: () => void;
};

export type NodePermissions = Acl;

/** Copy from Angular Material. */
export interface Sort {
    /** The id of the column being sorted. */
    active: string;
    /** The sort direction. */
    direction: 'asc' | 'desc' | '';
}
