import { Component } from '@angular/core';
import { PluginStatus } from '../../../core-module/core.module';
import { RestAdminService } from '../../../core-module/rest/services/rest-admin.service';

// Charts.js
declare var Chart: any;

@Component({
    selector: 'es-admin-plugins',
    templateUrl: 'plugins.component.html',
    styleUrls: ['plugins.component.scss'],
})
export class AdminPluginsComponent {
    plugins: PluginStatus[];
    constructor(private adminService: RestAdminService) {
        this.adminService.getPlugins().subscribe((plugins) => {
            this.plugins = plugins;
        });
    }
}
