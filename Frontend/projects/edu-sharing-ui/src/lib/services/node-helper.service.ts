import { Inject, Injectable, Optional } from '@angular/core';
import {
    ApiHelpersService,
    ConfigService,
    User,
    NetworkService,
    Node,
    ProposalNode,
    RestConstants,
} from 'ngx-edu-sharing-api';
import { TranslateService } from '@ngx-translate/core';
import * as Workflow from '../types/workflow';
import { RepoUrlService } from './repo-url.service';
import { Params } from '@angular/router';
import { UIConstants } from '../util/ui-constants';
import { ASSETS_BASE_PATH } from '../types/injection-tokens';
import { map } from 'rxjs/operators';
@Injectable({
    providedIn: 'root',
})
export class NodeHelperService {
    constructor(
        protected translate: TranslateService,
        protected apiHelpersService: ApiHelpersService,
        protected networkService: NetworkService,
        protected configService: ConfigService,
        protected repoUrlService: RepoUrlService,
        @Optional() @Inject(ASSETS_BASE_PATH) private assetsBasePath: string,
    ) {}

    public getCollectionScopeInfo(node: Node): { icon: string; scopeName: string } {
        const scope = node.collection ? node.collection.scope : null;
        let icon = 'help';
        let scopeName = 'UNKNOWN';
        if (scope === RestConstants.COLLECTIONSCOPE_MY) {
            icon = 'lock';
            scopeName = 'MY';
        }
        if (
            scope === RestConstants.COLLECTIONSCOPE_ORGA ||
            scope === RestConstants.COLLECTIONSCOPE_CUSTOM
        ) {
            icon = 'group';
            scopeName = 'SHARED';
        }
        if (
            scope === RestConstants.COLLECTIONSCOPE_ALL ||
            scope === RestConstants.COLLECTIONSCOPE_CUSTOM_PUBLIC
        ) {
            icon = 'language';
            scopeName = 'PUBLIC';
        }
        if (node.collection?.type === RestConstants.COLLECTIONTYPE_EDITORIAL) {
            icon = 'star';
            scopeName = 'TYPE_EDITORIAL';
        }
        if (node.collection?.type === RestConstants.COLLECTIONTYPE_MEDIA_CENTER) {
            icon = 'business';
            scopeName = 'TYPE_MEDIA_CENTER';
        }
        return { icon, scopeName };
    }
    /**
     * Return the license icon of a node
     */
    async getLicenseIcon(node: Node): Promise<string> {
        // prefer manual mapping instead of backend data to support custom states from local edits
        const license = node.properties?.[RestConstants.CCM_PROP_LICENSE]?.[0];
        if (license) {
            return this.getLicenseIconByString(license);
        }
        return node.license ? this.repoUrlService.getRepoUrl(node.license.icon, node) : null;
    }

    /**
     * Get a license icon by using the property value string
     */
    public getLicenseIconByString(string: String, useNoneAsFallback = true): string {
        let icon = string.replace(/_/g, '-').toLowerCase();
        if (icon == '') icon = 'none';

        const LICENSE_ICONS = [
            'cc-0',
            'cc-by-nc',
            'cc-by-nc-nd',
            'cc-by-nc-sa',
            'cc-by-nd',
            'cc-by-sa',
            'cc-by',
            'copyright-free',
            'copyright-license',
            'custom',
            'edu-nc-nd-noDo',
            'edu-nc-nd',
            'edu-p-nr-nd-noDo',
            'edu-p-nr-nd',
            'none',
            'pdm',
            'schulfunk',
            'unterrichts-und-lehrmedien',
        ];
        if (LICENSE_ICONS.indexOf(icon) == -1 && !useNoneAsFallback) return null; // icon='none';
        if (icon == 'none' && !useNoneAsFallback) return null;
        const result =
            this.apiHelpersService.getServerUrl() + '/../ccimages/licenses/' + icon + '.svg';
        return result;
    }
    /**
     * Return a translated name of a license name for a node
     * @param node
     * @param translate
     * @returns {string|any|string|any|string|any|string|any|string|any|string}
     */
    public getLicenseName(node: Node) {
        let prop = node.properties[RestConstants.CCM_PROP_LICENSE]?.[0];
        if (!prop) prop = '';
        return this.getLicenseNameByString(prop);
    }

    /**
     * Return a translated name for a license string
     * @param string
     * @param translate
     * @returns {any}
     */
    public getLicenseNameByString(name: string) {
        if (name == '') {
            name = 'NONE';
        }
        return this.translate.instant('LICENSE.NAMES.' + name);
        // return name.replace(/_/g,"-");
    }

    /**
     * return the License URL (e.g. for CC_BY licenses) for a license string and version
     * @param licenseProperty
     * @param licenseVersion
     * @param licenseLocale
     */
    public getLicenseUrlByString(
        licenseProperty: string,
        licenseVersion: string,
        licenseLocale: string,
    ) {
        const isV4 = licenseVersion === '4.0';
        const locale = isV4 || !licenseLocale ? '' : licenseLocale.toLowerCase() + '/';
        return this.translate
            .get(`LICENSE.URLS.${licenseProperty}`, {
                version: licenseVersion,
                locale: locale,
            })
            .pipe(
                map((url: string) => {
                    // when the translation fails it might return something like 'LICENSE.URLS.undefined'
                    if (!url || url.startsWith('LICENSE.URLS')) return null;
                    if (!isV4) {
                        // only the international 4.0 version supports different languages
                        // so this part needs to be removed for all other versions
                        url = url.replace('.de', '');
                    }
                    return url;
                }),
            );
    }

    public getWorkflowStatusById(id: string) {
        const workflows = this.getWorkflows();
        return workflows.filter((w) => w.id === id)?.[0];
    }
    public getWorkflowStatus(node: Node, useFromConfig = false): Workflow.WorkflowDefinitionStatus {
        let value = node.properties[RestConstants.CCM_PROP_WF_STATUS]?.[0];
        if (!value) {
            return this.getDefaultWorkflowStatus(useFromConfig);
        }
        return {
            current: this.getWorkflowStatusById(value),
            initial: this.getWorkflowStatusById(value),
        };
    }
    getDefaultWorkflowStatus(useFromConfig = false): Workflow.WorkflowDefinitionStatus {
        const result = {
            current: null as Workflow.WorkflowDefinition,
            initial: null as Workflow.WorkflowDefinition,
        };
        result.initial = this.getWorkflows()[0];
        let defaultStatus: string = null;
        if (useFromConfig) {
            defaultStatus = this.configService.instant('workflow.defaultStatus');
        }
        if (defaultStatus) {
            result.current = this.getWorkflows().find((w) => w.id === defaultStatus);
        } else {
            result.current = result.initial;
        }
        return result;
    }
    getWorkflows(): Workflow.WorkflowDefinition[] {
        return this.configService.instant('workflow.workflows', [
            Workflow.WORKFLOW_STATUS_UNCHECKED,
            Workflow.WORKFLOW_STATUS_TO_CHECK,
            Workflow.WORKFLOW_STATUS_HASFLAWS,
            Workflow.WORKFLOW_STATUS_CHECKED,
        ]);
    }
    copyDataToNode<T extends Node | User>(target: T, source: T) {
        target.properties = source.properties;
        (target as Node).name = (source as Node).name;
        (target as Node).title = (source as Node).title;
        (target as User).authorityName = (source as User).authorityName;
        (target as User).profile = (source as User).profile;
        (target as User).status = (source as User).status;
    }
    isNodeCollection(node: Node): boolean {
        return node.aspects?.includes(RestConstants.CCM_ASPECT_COLLECTION) || !!node.collection;
    }
    public getSourceIconPath(src: string) {
        return (this.assetsBasePath ?? '') + 'assets/images/sources/' + src.toLowerCase() + '.png';
    }

    getNodeLink(mode: 'routerLink' | 'queryParams', node: Node) {
        if (!node?.ref) {
            return null;
        }
        let data: { routerLink: string; queryParams: Params } = null;
        if (this.isNodeCollection(node)) {
            data = {
                routerLink: UIConstants.ROUTER_PREFIX + 'collections',
                queryParams: { id: node.ref.id },
            };
        } else {
            if (node.isDirectory) {
                let path;
                if (
                    node.properties?.[RestConstants.CCM_PROP_EDUSCOPENAME]?.[0] ===
                    RestConstants.SAFE_SCOPE
                ) {
                    path = UIConstants.ROUTER_PREFIX + 'workspace/safe';
                } else {
                    path = UIConstants.ROUTER_PREFIX + 'workspace';
                }
                data = {
                    routerLink: path,
                    queryParams: { id: node.ref.id },
                };
            } else if (node.ref) {
                const fromHome = this.networkService.isFromHomeRepository(node);
                data = {
                    routerLink: UIConstants.ROUTER_PREFIX + 'render/' + node.ref.id,
                    queryParams: {
                        repository: fromHome ? null : node.ref.repo,
                        proposal: (node as ProposalNode).proposal?.ref.id,
                        proposalCollection: (node as ProposalNode).proposalCollection?.ref.id,
                    },
                };
            }
        }
        if (data === null) {
            return '';
        }
        if (mode === 'routerLink') {
            return '/' + data.routerLink;
        }
        // enforce clearing of parameters which should only be consumed once
        data.queryParams.redirectFromSSO = null;
        return data.queryParams;
    }
}
