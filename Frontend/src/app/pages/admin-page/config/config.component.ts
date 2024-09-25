import { Component, HostListener } from '@angular/core';
import { ConfigService } from 'ngx-edu-sharing-api';
import { forkJoin } from 'rxjs';
import { RestAdminService } from '../../../core-module/rest/services/rest-admin.service';
import { Toast } from '../../../services/toast';
import { Closable } from '../../../features/dialogs/card-dialog/card-dialog-config';
import { DialogsService } from '../../../features/dialogs/dialogs.service';

@Component({
    selector: 'es-admin-config',
    templateUrl: 'config.component.html',
    styleUrls: ['config.component.scss'],
})
export class AdminConfigComponent {
    public static CONFIG_FILE_REFERENCE = 'edu-sharing.reference.conf';
    public static EXTENSION_CONFIG_FILE = 'edu-sharing.application.conf';
    public static OVERRIDE_CONFIG_FILE = 'edu-sharing.override.conf';
    public static CONFIG_DEPLOYMENT_FILE = 'edu-sharing.deployment.conf';
    public static CLIENT_CONFIG_FILE = 'client.config.xml';

    @HostListener('keydown.control.s', ['$event']) onCtrlS(event: KeyboardEvent) {
        event.preventDefault();
        this.save();
    }

    codeOptionsHocoonRO: any = {
        minimap: { enabled: false },
        language: 'json',
        readOnly: true,
        automaticLayout: true,
    };
    codeOptionsHocoonRW: any = {
        minimap: { enabled: false },
        language: 'json',
        automaticLayout: true,
    };
    clientCodeOptions: any = {
        minimap: { enabled: false },
        language: 'xml',
        automaticLayout: true,
    };
    configs: any = {
        clientConfig: null,
        reference: null,
        clusterDeployment: null,
        defaultsOverride: null,
        nodeDeployment: null,
        extension: null,
        clusterOverride: null,
        nodeOverride: null,
        parsed: null,
    };
    size = 'medium';
    showRO = false;
    editSupported = false;

    constructor(
        private adminService: RestAdminService,
        private configService: ConfigService,
        private dialogs: DialogsService,
        private toast: Toast,
    ) {
        this.adminService
            .getConfigFile(AdminConfigComponent.CLIENT_CONFIG_FILE, 'DEFAULTS')
            .subscribe((data) => (this.configs.clientConfig = data));
        this.adminService
            .getConfigFile(AdminConfigComponent.CONFIG_FILE_REFERENCE, 'DEFAULTS')
            .subscribe((base) => (this.configs.reference = base));
        this.adminService
            .getConfigFile(AdminConfigComponent.EXTENSION_CONFIG_FILE, 'DEFAULTS')
            .subscribe((deployment) => (this.configs.extension = deployment));
        this.adminService
            .getConfigFile(AdminConfigComponent.CONFIG_DEPLOYMENT_FILE, 'CLUSTER')
            .subscribe((deployment) => (this.configs.clusterDeployment = deployment));
        this.adminService
            .getConfigFile(AdminConfigComponent.CONFIG_DEPLOYMENT_FILE, 'NODE')
            .subscribe((deployment) => (this.configs.nodeDeployment = deployment));
        this.adminService
            .getConfigFile(AdminConfigComponent.OVERRIDE_CONFIG_FILE, 'DEFAULTS')
            .subscribe((deployment) => (this.configs.defaultsOverride = deployment));
        this.adminService
            .getConfigFile(AdminConfigComponent.OVERRIDE_CONFIG_FILE, 'CLUSTER')
            .subscribe((c) => (this.configs.clusterOverride = c));
        this.adminService
            .getConfigFile(AdminConfigComponent.OVERRIDE_CONFIG_FILE, 'NODE')
            .subscribe((c) => (this.configs.nodeOverride = c));
        this.adminService.getConfigMerged().subscribe(
            (merged) => {
                this.configs.parsed = JSON.stringify(merged, null, 2);
                this.setEditSupported(merged?.security?.configuration?.inlineEditing);
            },
            () => {
                this.setEditSupported(false);
            },
        );
    }
    setEditSupported(status: boolean) {
        this.editSupported = status;
        this.showRO = !this.editSupported;
        // fix: monaco editor requires full object change to trigger/sync state
        this.codeOptionsHocoonRW = { ...this.codeOptionsHocoonRW, readOnly: !this.editSupported };
        this.clientCodeOptions = { ...this.clientCodeOptions, readOnly: !this.editSupported };
    }
    private displayError(error: any) {
        console.warn(error);
        this.toast.closeProgressSpinner();
        void this.dialogs.openGenericDialog({
            title: 'ADMIN.GLOBAL_CONFIG.ERROR',
            message: 'ADMIN.GLOBAL_CONFIG.PARSE_ERROR',
            messageParameters: { error: error?.error?.error },
            messageMode: 'html',
            closable: Closable.Disabled,
            buttons: [{ label: 'ADMIN.GLOBAL_CONFIG.CHECK', config: { color: 'danger' } }],
        });
    }
    save() {
        this.toast.showProgressSpinner();
        forkJoin([
            this.adminService.updateConfigFile(
                AdminConfigComponent.CLIENT_CONFIG_FILE,
                'DEFAULTS',
                this.configs.clientConfig,
            ),
            this.adminService.updateConfigFile(
                AdminConfigComponent.EXTENSION_CONFIG_FILE,
                'DEFAULTS',
                this.configs.extension,
            ),
            this.adminService.updateConfigFile(
                AdminConfigComponent.OVERRIDE_CONFIG_FILE,
                'CLUSTER',
                this.configs.clusterOverride,
            ),
            this.adminService.updateConfigFile(
                AdminConfigComponent.OVERRIDE_CONFIG_FILE,
                'NODE',
                this.configs.nodeOverride,
            ),
        ]).subscribe(() => {
            this.adminService.refreshAppInfo().subscribe(
                () => {
                    this.toast.closeProgressSpinner();
                    this.configService.observeConfig({ forceUpdate: true }).subscribe(
                        () => {
                            this.toast.closeProgressSpinner();
                            this.toast.toast('ADMIN.GLOBAL_CONFIG.SAVED');
                        },
                        (error) => this.displayError(error),
                    );
                },
                (error) => this.displayError(error),
            );
        });
    }
}
