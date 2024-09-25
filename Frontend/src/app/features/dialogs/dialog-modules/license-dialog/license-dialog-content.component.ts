import {
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnInit,
    Output,
    ViewChild,
} from '@angular/core';
import { LicenseDialogData, LicenseDialogResult } from './license-dialog-data';

import { TranslateService } from '@ngx-translate/core';
import { Acl, Values } from 'dist/edu-sharing-api/lib/api/models';
import { forkJoin } from 'rxjs';
import { map } from 'rxjs/operators';
import {
    ConfigurationService,
    DialogButton,
    LocalPermissionsResult,
    Node,
    NodePermissions,
    RestConnectorService,
    RestConstants,
    RestHelper,
    RestIamService,
    RestNodeService,
} from '../../../../core-module/core.module';
import { NodeHelperService } from '../../../../services/node-helper.service';
import { Toast } from '../../../../services/toast';
import { UIHelper } from '../../../../core-ui-module/ui-helper';
import { UserPresentableError } from '../../../../features/mds/mds-editor/mds-editor-common.service';
import { MdsEditorInstanceService } from '../../../../features/mds/mds-editor/mds-editor-instance.service';
import { ViewInstanceService } from '../../../../features/mds/mds-editor/mds-editor-view/view-instance.service';
import { MdsEditorWidgetAuthorComponent } from '../../../../features/mds/mds-editor/widgets/mds-editor-widget-author/mds-editor-widget-author.component';
import { NodeService } from 'ngx-edu-sharing-api';

const ALL_LICENSE_TYPES = [
    'NONE',
    'CC_0',
    'CC_BY',
    'SCHULFUNK',
    'UNTERRICHTS_UND_LEHRMEDIEN',
    'COPYRIGHT',
    'CUSTOM',
];

const ALL_COUNTRIES = [
    'AE',
    'AL',
    'AR',
    'AT',
    'AU',
    'BA',
    'BG',
    'BH',
    'BO',
    'BR',
    'BY',
    'CA',
    'CH',
    'CL',
    'CN',
    'CO',
    'CR',
    'CY',
    'CZ',
    'DE',
    'DK',
    'DO',
    'DZ',
    'EC',
    'EE',
    'EG',
    'ES',
    'FI',
    'FR',
    'GB',
    'GR',
    'GT',
    'HK',
    'HN',
    'HR',
    'HU',
    'ID',
    'IE',
    'IL',
    'IN',
    'IQ',
    'IS',
    'IT',
    'JO',
    'JP',
    'KR',
    'KW',
    'LB',
    'LT',
    'LU',
    'LV',
    'LY',
    'MA',
    'ME',
    'MK',
    'MT',
    'MX',
    'MY',
    'NI',
    'NL',
    'NO',
    'NZ',
    'OM',
    'PA',
    'PE',
    'PH',
    'PL',
    'PR',
    'PT',
    'PY',
    'QA',
    'RO',
    'RS',
    'RU',
    'SA',
    'SD',
    'SE',
    'SG',
    'SI',
    'SK',
    'SY',
    'TH',
    'TN',
    'TR',
    'TW',
    'UA',
    'US',
    'UY',
    'VE',
    'VN',
    'YE',
    'ZA',
];

@Component({
    selector: 'es-license-dialog-content',
    templateUrl: './license-dialog-content.component.html',
    styleUrls: ['./license-dialog-content.component.scss'],
    providers: [MdsEditorInstanceService, ViewInstanceService],
})
export class LicenseDialogContentComponent implements OnInit {
    @ViewChild('selectLicense') selectLicense: ElementRef;
    @ViewChild('author') author: MdsEditorWidgetAuthorComponent;

    @Input() data: LicenseDialogData;

    /**
     * Emits the updated node or properties (depending on the provided `data`) when saved and null
     * when canceled.
     */
    @Output() done = new EventEmitter<LicenseDialogResult>();
    @Output() isLoading = new EventEmitter<boolean>();
    @Output() canSave = new EventEmitter<boolean>();

    set primaryType(primaryType: string) {
        if (!primaryType.startsWith('CC_BY')) {
            // make sure to unset cc-values if they're inapplicable
            this.ccCountry = '';
            this.ccVersion = '';
            this.ccShare = '';
            this.ccCommercial = '';
            if (!primaryType.startsWith('CC')) {
                this.ccProfileUrl = '';
                this.ccSourceUrl = '';
                this.ccTitleOfWork = '';
            } else if (primaryType == 'CC_0') {
                // CC0 is only available in version 1.0
                this.ccVersion = '1.0';
            }
        } else {
            // if no version is selected select 4.0 by default
            if (!this.ccVersion) this.ccVersion = '4.0';
        }
        this._primaryType = primaryType;
        this.updateCanSave();
    }
    get primaryType() {
        return this._primaryType;
    }
    set type(type: string) {
        if (type == 'CC_0' || type == 'PDM') {
            this.cc0Type = type;
            type = 'CC_0';
        }
        if (type == 'CC_BY') {
            this.ccCommercial = '';
            this.ccShare = '';
        }
        if (type == 'CC_BY_SA') {
            type = 'CC_BY';
            this.ccShare = 'SA';
        }
        if (type.startsWith('COPYRIGHT')) {
            this.copyrightType = type;
            type = 'COPYRIGHT';
        }
        this.primaryType = type;
    }
    get type() {
        return this.getLicenseProperty();
    }
    get getccCountries() {
        return this._ccCountries;
    }
    set oerMode(oerMode: boolean) {
        this._oerMode = oerMode;
        this.showCcAuthor = false;
        if (oerMode) {
            if (this.isOerLicense()) {
                return;
            } else {
                this.type = 'NONE';
            }
        }
    }
    get oerMode() {
        return this._oerMode;
    }

    _properties: any;
    ccShare = '';
    ccCommercial = '';
    ccTitleOfWork = '';
    ccSourceUrl = '';
    ccVersion = '4.0';
    ccCountry = '';
    ccProfileUrl = '';
    copyrightType = 'COPYRIGHT_FREE';
    rightsDescription = '';

    oerAvailable = true;
    _ccCountries: Array<{ key: string; name: string }> = [];
    licenseMainTypes: string[];
    private _primaryType = '';
    private buttons: DialogButton[];
    private cc0Type = 'CC_0';
    private eduType = 'P_NR';
    private showCcAuthor = false; // FIXME: not used.
    private contact = true;
    private contactIndeterminate = false;
    private release = false;
    private releaseIndeterminate = false;
    private eduDownload = true;
    private _oerMode = true;
    private permissions: Acl;
    private allowedLicenses: string[];
    private releaseMulti: string;
    private allowRelease = true; // FIXME: not used.

    constructor(
        private connector: RestConnectorService,
        private translate: TranslateService,
        private config: ConfigurationService,
        private nodeHelper: NodeHelperService,
        private mdsEditorInstanceService: MdsEditorInstanceService,
        private iamApi: RestIamService,
        private toast: Toast,
        private nodeApi: RestNodeService,
        private nodeService: NodeService,
    ) {}

    ngOnInit(): void {
        this.translateLicenseCountries(ALL_COUNTRIES);
        void this.iamApi.getCurrentUserAsync();
        switch (this.data.kind) {
            case 'nodes':
                this.initNodes(this.data.nodes);
                break;
            case 'properties':
                this.initProperties(this.data.properties);
                break;
        }
    }

    private updateCanSave() {
        this.canSave.emit(this.type !== 'MULTI');
    }

    private initNodes(nodesIn: Node[]) {
        this.isLoading.emit(true);
        this.loadNodes(nodesIn).subscribe(
            async (nodes) => {
                try {
                    await this.mdsEditorInstanceService.initWithNodes(nodes);
                } catch (e) {
                    if (e instanceof UserPresentableError || e.message) {
                        this.toast.error(null, e.message);
                    } else {
                        this.toast.error(e);
                    }
                    this.done.emit(null);
                    return;
                }
                this.loadConfig();
                this.checkAllowRelease();
                this.readLicense();
                this.setDefaultModeState();
                this.isLoading.emit(false);
                this.updateCanSave();
                this.releaseMulti = null;
                let i = 0;
                for (const node of nodes) {
                    i++;
                    this.nodeService.getPermissions(node.ref.id).subscribe((permissions) => {
                        this.permissions = permissions.localPermissions;
                        this.readPermissions(i == this.getNodes()?.length);
                    });
                }
            },
            (error) => {
                this.toast.error(error);
                this.done.emit(null);
            },
        );
    }

    private initProperties(properties: any) {
        this.loadConfig();
        this._properties = properties;
        this.readLicense();
        this.setDefaultModeState();
        this.mdsEditorInstanceService.initWithNodes(
            [
                {
                    properties,
                } as any,
            ],
            { refetch: false },
        );
        this.isLoading.emit(false);
        this.updateCanSave();
    }

    isAllowedLicense(license: string) {
        return this.allowedLicenses == null || this.allowedLicenses.indexOf(license) != -1;
    }

    isOerLicense() {
        return (
            this.getLicenseProperty() == 'CC_0' ||
            this.getLicenseProperty() == 'PDM' ||
            this.getLicenseProperty() == 'CC_BY' ||
            this.getLicenseProperty() == 'CC_BY_SA'
        );
    }

    private loadNodes(nodes: Node[]) {
        return forkJoin(
            nodes.map((n) =>
                this.nodeApi
                    .getNodeMetadata(n.ref.id, [RestConstants.ALL])
                    .pipe(map((n2) => n2.node)),
            ),
        );
    }

    private loadConfig() {
        this.config.get('allowedLicenses').subscribe((data: string[]) => {
            if (!data) {
                this.licenseMainTypes = ALL_LICENSE_TYPES.slice();
                this.allowedLicenses = null;
            } else {
                this.licenseMainTypes = [];
                this.allowedLicenses = data;
                if (!this.oerAvailable) {
                    this.oerMode = false;
                }
                for (const entry of data) {
                    if (entry.startsWith('CC_BY')) {
                        if (this.licenseMainTypes.indexOf('CC_BY') == -1)
                            this.licenseMainTypes.push('CC_BY');
                    } else if (entry == 'CC_0' || entry == 'PDM') {
                        if (this.licenseMainTypes.indexOf('CC_0') == -1)
                            this.licenseMainTypes.push('CC_0');
                    } else if (entry.startsWith('COPYRIGHT')) {
                        this.licenseMainTypes.push('COPYRIGHT');
                        if (data.indexOf(this.copyrightType) == -1) this.copyrightType = entry;
                    } else if (ALL_LICENSE_TYPES.indexOf(entry) != -1) {
                        this.licenseMainTypes.push(entry);
                    }
                }
            }
            for (const license of this.config.instant('customLicenses', [])) {
                this.licenseMainTypes.splice(
                    license.position >= 0
                        ? license.position
                        : this.licenseMainTypes.length - license.position,
                    0,
                    license.id,
                );
            }
        });
    }

    async saveLicense() {
        if (this.type === 'CUSTOM' && !this.rightsDescription.trim()) {
            this.toast.error(null, 'LICENSE.DESCRIPTION_REQUIRED');
            return;
        }
        if (this._properties) {
            // this._properties is set through getProperties, which is currently only used here and
            // in EmbedComponent, which controls saving the changes on its own, so just return here
            // fixme: if possible and worth it: unify (probably by letting license-dialog handle the
            //  saving, so all we do here is emit;
            //  on the other hand: having the dialog emit "done" only when everything went through
            //  without error, as it currently is, is also nice)
            this.done.emit(await this.getProperties(this._properties));
            return;
        }
        if (!this.getLicenseProperty() && this.release) {
            // this.toast.error(null,'WORKSPACE.LICENSE.RELEASE_WITHOUT_LICENSE');
            // return;
        }
        let prop: Values = {};

        prop = await this.getProperties(prop);
        this.isLoading.emit(true);
        const updatedNodes: Node[] = [];
        for (const node of this.getNodes()) {
            node.properties = prop;
            this.nodeApi
                .editNodeMetadataNewVersion(node.ref.id, RestConstants.COMMENT_LICENSE_UPDATE, prop)
                .subscribe(
                    (result) => {
                        updatedNodes.push(result.node);
                        this.savePermissions(node);
                        if (updatedNodes.length === this.getNodes().length) {
                            this.toast.toast('WORKSPACE.TOAST.LICENSE_UPDATED');
                            this.isLoading.emit(false);
                            this.done.emit(updatedNodes);
                        }
                    },
                    (error: any) => {
                        this.isLoading.emit(false);
                        this.toast.error(error);
                    },
                );
        }
    }

    private getNodes() {
        return this.mdsEditorInstanceService.nodes$.value;
    }

    private getValueForAll(
        prop: string,
        fallbackNotIdentical: any = '',
        fallbackIsEmpty = fallbackNotIdentical,
        asArray = false,
    ) {
        if (this._properties) {
            return this._properties[prop] ? this._properties[prop][0] : fallbackIsEmpty;
        }
        if (this.getNodes()) {
            return this.nodeHelper.getValueForAll(
                this.getNodes(),
                prop,
                fallbackNotIdentical,
                fallbackIsEmpty,
                asArray,
            );
        } else {
            return fallbackIsEmpty;
        }
    }

    private readLicense() {
        let license = this.getValueForAll(RestConstants.CCM_PROP_LICENSE, 'MULTI', 'NONE');
        if (!license) license = 'NONE';
        this.type = license;
        if (license.startsWith('CC_BY')) {
            this.type = 'CC_BY';
            if (license.indexOf('SA') != -1) this.ccShare = 'SA';
            if (license.indexOf('ND') != -1) this.ccShare = 'ND';
            if (license.indexOf('NC') != -1) this.ccCommercial = 'NC';

            this.ccTitleOfWork = this.getValueForAll(RestConstants.CCM_PROP_LICENSE_TITLE_OF_WORK);
            this.ccSourceUrl = this.getValueForAll(RestConstants.CCM_PROP_LICENSE_SOURCE_URL);
            this.ccProfileUrl = this.getValueForAll(RestConstants.CCM_PROP_LICENSE_PROFILE_URL);
            this.ccVersion = this.getValueForAll(
                RestConstants.CCM_PROP_LICENSE_CC_VERSION,
                this.ccVersion,
            );
            this.ccCountry = this.getValueForAll(RestConstants.CCM_PROP_LICENSE_CC_LOCALE);
        }
        if (license == 'CC_0') {
            this.type = 'CC_0';
        }
        if (license == 'PDM') {
            this.type = 'PDM';
        }
        if (license.startsWith('COPYRIGHT')) {
            this.type = 'COPYRIGHT';
            this.copyrightType = license;
        }
        if (license == 'SCHULFUNK') {
            this.type = license;
        }
        if (license.startsWith('EDU')) {
            this.type = 'EDU';
            if (license.indexOf('P_NR') != -1) this.eduType = 'P_NR';
            if (license.indexOf('NC') != -1) this.eduType = 'NC';
            this.eduDownload = license.indexOf('ND') == -1;
        }
        if (license == 'CUSTOM') this.type = license;

        this.rightsDescription = this.getValueForAll(RestConstants.LOM_PROP_RIGHTS_DESCRIPTION);
        const contactState = this.getValueForAll(
            RestConstants.CCM_PROP_QUESTIONSALLOWED,
            'multi',
            'true',
        );
        this.contact = contactState == 'true' || contactState == true;
        UIHelper.invalidateMaterializeTextarea('authorFreetext');
        UIHelper.invalidateMaterializeTextarea('licenseRights');
        this.contactIndeterminate = contactState == 'multi';
    }

    getLicenseProperty() {
        let name = this.primaryType;
        if (this.primaryType == 'NONE') return '';
        if (this.primaryType == 'CC_BY') {
            if (this.ccCommercial) name += '_' + this.ccCommercial;
            if (this.ccShare) name += '_' + this.ccShare;
            return name;
        }
        if (this.primaryType == 'CC_0') {
            return this.cc0Type;
        }
        if (this.primaryType == 'COPYRIGHT') {
            return this.copyrightType;
        }
        if (this.primaryType == 'EDU') {
            name += '_' + this.eduType;
            if (!this.eduDownload) name += '_ND';
        }

        return name;
    }

    getLicenseName() {
        return this.nodeHelper.getLicenseNameByString(this.getLicenseProperty());
    }

    getLicenseUrl() {
        return this.nodeHelper.getLicenseUrlByString(
            this.getLicenseProperty(),
            this.ccVersion,
            this.ccCountry,
        );
    }

    getLicenseUrlVersion(type: string) {
        return this.nodeHelper.getLicenseUrlByString(type, this.ccVersion, this.ccCountry);
    }

    getLicenseIcon() {
        return this.nodeHelper.getLicenseIconByString(this.getLicenseProperty());
    }

    private savePermissions(node: Node) {
        if (this.releaseIndeterminate) {
            return;
        }
        let add = true;
        let index = 0;
        for (const perm of this.permissions.permissions) {
            if (perm.authority.authorityName == RestConstants.AUTHORITY_EVERYONE) {
                add = false;
                if (
                    perm.permissions.indexOf(RestConstants.ACCESS_CC_PUBLISH) == -1 &&
                    this.release
                ) {
                    perm.permissions.push(RestConstants.ACCESS_CC_PUBLISH);
                }
                if (perm.permissions.indexOf(RestConstants.ACCESS_CONSUMER) == -1 && this.release) {
                    perm.permissions.push(RestConstants.ACCESS_CONSUMER);
                }
                /*if(perm.permissions.indexOf(RestConstants.ACCESS_CC_PUBLISH)!=-1 && !this.release){
              perm.permissions.splice(perm.permissions.indexOf(RestConstants.ACCESS_CC_PUBLISH),1);
            }
            if(perm.permissions.indexOf(RestConstants.ACCESS_CONSUMER)!=-1 && !this.release){
              perm.permissions.splice(perm.permissions.indexOf(RestConstants.ACCESS_CONSUMER),1);
            }
            */
                break;
            }
            index++;
        }
        // remove all_authorities
        if (!add && !this.release) {
            this.permissions.permissions.splice(index, 1);
        }
        // add all_authorities
        if (add && this.release) {
            const perm = RestHelper.getAllAuthoritiesPermission();
            perm.permissions = [RestConstants.ACCESS_CC_PUBLISH, RestConstants.ACCESS_CONSUMER];
            this.permissions.permissions.push(perm);
        }
        const permissions = RestHelper.copyAndCleanPermissions(
            this.permissions.permissions,
            this.permissions.inherited,
        );
        this.nodeService
            .setPermissions(node.ref.id, permissions, { sendMail: false, sendCopy: false })
            .subscribe(
                () => {},
                (error: any) => this.toast.error(error),
            );
    }

    private readPermissions(last: boolean) {
        this.release = false;
        if (this)
            for (const perm of this.permissions.permissions) {
                if (
                    perm.authority.authorityType == RestConstants.AUTHORITY_TYPE_EVERYONE &&
                    perm.permissions.indexOf(RestConstants.ACCESS_CC_PUBLISH) != -1
                ) {
                    if (this.releaseMulti != null && this.releaseMulti != 'true')
                        this.releaseMulti = 'multi';
                    else this.releaseMulti = 'true';
                    if (last) this.setPermissionState();
                    return;
                }
            }
        if (this.releaseMulti != null && this.releaseMulti != 'false') this.releaseMulti = 'multi';
        else this.releaseMulti = 'false';
        if (last) this.setPermissionState();
    }

    private setPermissionState() {
        if (this.releaseMulti == 'true') this.release = true;
        if (this.releaseMulti == null || this.releaseMulti == 'false') this.release = false;

        if (this.releaseMulti == 'multi') {
            this.releaseIndeterminate = true;
        }
    }

    private checkAllowRelease() {
        this.connector
            .hasToolPermission(RestConstants.TOOLPERMISSION_INVITE_ALLAUTHORITIES)
            .subscribe((data: boolean) => {
                if (!this.getNodes()) {
                    return;
                }
                if (!data) {
                    this.allowRelease = false;
                    return;
                }
                for (const node of this.getNodes()) {
                    if (node.access.indexOf(RestConstants.ACCESS_CHANGE_PERMISSIONS) == -1) {
                        this.allowRelease = false;
                        return;
                    }
                }
            });
    }

    async getProperties(prop = this._properties) {
        prop[RestConstants.CCM_PROP_LICENSE] = [this.getLicenseProperty()];
        if (!this.contactIndeterminate)
            prop[RestConstants.CCM_PROP_QUESTIONSALLOWED] = [this.contact];
        if (this.isCCAttributableLicense()) {
            prop[RestConstants.CCM_PROP_LICENSE_TITLE_OF_WORK] = [];
            prop[RestConstants.CCM_PROP_LICENSE_SOURCE_URL] = [];
            prop[RestConstants.CCM_PROP_LICENSE_PROFILE_URL] = [];
            prop[RestConstants.CCM_PROP_LICENSE_CC_VERSION] = [];
            prop[RestConstants.CCM_PROP_LICENSE_CC_LOCALE] = [];
            if (this.ccTitleOfWork) {
                prop[RestConstants.CCM_PROP_LICENSE_TITLE_OF_WORK] = [this.ccTitleOfWork];
            }
            if (this.ccSourceUrl) {
                prop[RestConstants.CCM_PROP_LICENSE_SOURCE_URL] = [this.ccSourceUrl];
            }
            if (this.ccProfileUrl) {
                prop[RestConstants.CCM_PROP_LICENSE_PROFILE_URL] = [this.ccProfileUrl];
            }
            if (this.ccVersion) {
                prop[RestConstants.CCM_PROP_LICENSE_CC_VERSION] = [this.ccVersion];
                // make sure v4.0 has no locale
                if (this.ccVersion === '4.0') {
                    this.ccCountry = '';
                }
            }
            if (this.ccCountry) {
                prop[RestConstants.CCM_PROP_LICENSE_CC_LOCALE] = [this.ccCountry];
            }
        }
        prop = this.author
            ? await this.author.getValues(
                  prop,
                  this.getNodes()?.length === 1 ? this.getNodes()[0] : null,
              )
            : prop;

        if (this.type == 'CUSTOM') {
            prop[RestConstants.LOM_PROP_RIGHTS_DESCRIPTION] = [this.rightsDescription];
        }
        return prop;
    }

    isCCAttributableLicense() {
        return this.getLicenseProperty() && this.getLicenseProperty().startsWith('CC_BY');
    }

    /**
     * Get all the key from countries and return the array with key and name (Translated)
     * @param {string[]} countries array with all Countries Key
     */
    private translateLicenseCountries(countries: string[]) {
        // fixme: many possible combinations of countries and versions do not actually exist
        //        we could use a dict here to collect the versions available per country and then
        //        only show the available versions and vice versa only the available countries
        //        per version
        this._ccCountries = [];
        countries.forEach((country) => {
            this._ccCountries.push({
                key: country,
                name: this.translate.instant('COUNTRY_CODE.' + country),
            });
        });
        this._ccCountries.sort((a, b) => this.sortCountries({ a: a.name, b: b.name }));
    }

    /**
     * Function wich compare 2 string and return one of those numbers -1,0,1
     *
     *   -1 if a<b
     *    1 if a>b
     *    0 if a=b
     *
     * @param {string} a first string
     * @param {string} b second string
     * @returns {number}   -1 | 0 | 1
     */
    private sortCountries({ a, b }: { a: string; b: string }): number {
        if (a.toLowerCase() < b.toLowerCase()) return -1;
        if (a.toLowerCase() > b.toLowerCase()) return 1;
        return 0;
    }

    hasMixedAuthorValues() {
        return (
            this.getNodes() != null &&
            (this.nodeHelper.hasMixedPropertyValues(
                this.getNodes(),
                RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR,
            ) ||
                this.nodeHelper.hasMixedPropertyValues(
                    this.getNodes(),
                    RestConstants.CCM_PROP_AUTHOR_FREETEXT,
                ))
        );
    }

    resetMixedAuthorValues() {
        if (
            this.nodeHelper.hasMixedPropertyValues(
                this.getNodes(),
                RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR,
            )
        ) {
            this.getNodes().forEach(
                (n) => (n.properties[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR] = null),
            );
        }
        if (
            this.nodeHelper.hasMixedPropertyValues(
                this.getNodes(),
                RestConstants.CCM_PROP_AUTHOR_FREETEXT,
            )
        ) {
            this.getNodes().forEach(
                (n) => (n.properties[RestConstants.CCM_PROP_AUTHOR_FREETEXT] = null),
            );
        }
    }

    private setDefaultModeState() {
        this.oerAvailable =
            !this.allowedLicenses || this.allowedLicenses.filter((e) => e !== 'NONE').length > 0;
        this.oerMode = this.oerAvailable && (this.isOerLicense() || this.primaryType == 'NONE');
    }
}
