import { Component, EventEmitter, Input, Output } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { DialogButton, SessionStorageService } from '../../../core-module/core.module';
import { Toast } from '../../../services/toast';
import { DialogsService } from '../../../features/dialogs/dialogs.service';

interface LuceneTemplate {
    query: string;
    properties: string;
    isDefault?: boolean;
}

type LuceneTemplates = { [key: string]: LuceneTemplate };

@Component({
    selector: 'es-lucene-template-memory',
    templateUrl: './lucene-template-memory.component.html',
    styleUrls: ['./lucene-template-memory.component.scss'],
})
export class LuceneTemplateMemoryComponent {
    private static readonly STORAGE_KEY = 'admin_lucene_templates';
    private static readonly DEFAULT_TEMPLATES: LuceneTemplates = {
        GROUPS: {
            query: 'TYPE:"cm:authorityContainer"',
            properties: [
                'cm:authorityDisplayName',
                'cm:authorityName',
                'ccm:groupType',
                'ccm:groupEmail',
                'ccm:groupScope',
                'ccm:groupSource',
            ].join('\n'),
            isDefault: true,
        },
        ORGS: {
            query: 'TYPE:"cm:authorityContainer"',
            properties: [
                'cm:authorityDisplayName',
                'cm:authorityName',
                'ccm:edu_homedir',
                'ccm:groupType',
                'ccm:groupEmail',
                'ccm:groupScope',
                'ccm:groupSource',
            ].join('\n'),
            isDefault: true,
        },
        PERSONS: {
            query: 'TYPE:"cm:person"',
            properties: [
                'cm:userName',
                'cm:firstName',
                'cm:lastName',
                'cm:email',
                'cm:esuid',
                'cm:homeFolder',
                'cm:esLastLogin',
            ].join('\n'),
            isDefault: true,
        },
        CREATED_CONTENTS_BY_PERSON: {
            query: '@cm\\:creator:"user" OR @cm\\:modifier:"user"',
            properties: [
                'sys:node-uuid',
                'cm:name',
                'cclom:title',
                'cm:creator',
                'cm:created',
                'cm:modifier',
                'cm:modified',
                'cclom:general_keyword',
                'ccm:comment_content',
            ].join('\n'),
            isDefault: true,
        },
        BROKEN_LINKS: {
            query: 'ISNOTNULL:"ccm:location_status" AND NOT @ccm\\:location_status:"200"',
            properties: [
                'sys:node-uuid',
                'cm:name',
                'cclom:title',
                'cm:created',
                'cm:modified',
                'cclom:general_keyword',
                'cclom:location',
                'ccm:replicationsource',
                'ccm:replicationsourceid',
            ].join('\n'),
            isDefault: true,
        },
    };

    @Input() query: string;
    @Input() properties: string;

    @Output() queryChange = new EventEmitter<string>();
    @Output() propertiesChange = new EventEmitter<string>();

    templates: LuceneTemplates;
    selectedTemplate: string;
    isNewTemplateDialogVisible = false;
    newTemplateName: string;

    readonly newTemplateDialogButtons = [
        new DialogButton('CLOSE', { color: 'standard' }, () => {
            this.closeNewTemplateDialog();
        }),
        new DialogButton(
            'ADMIN.BROWSER.LUCENE_TEMPLATE_MEMORY.CREATE_BUTTON',
            { color: 'primary' },
            () => this.createNewTemplate(),
        ),
    ];

    constructor(
        private dialogs: DialogsService,
        private storage: SessionStorageService,
        private toast: Toast,
        private translate: TranslateService,
    ) {
        this.storage
            .get(LuceneTemplateMemoryComponent.STORAGE_KEY)
            .subscribe((templates: LuceneTemplates) => {
                this.templates = templates ?? {};
                for (const key of Object.keys(LuceneTemplateMemoryComponent.DEFAULT_TEMPLATES)) {
                    this.templates[
                        this.translate.instant('ADMIN.BROWSER.LUCENE_DEFAULT_TEMPLATES.' + key)
                    ] = LuceneTemplateMemoryComponent.DEFAULT_TEMPLATES[key];
                }
            });
    }

    createNewTemplate(): void {
        if (!this.newTemplateName) {
            // Do nothing
        } else if (this.newTemplateName in this.templates) {
            if (this.templates[this.newTemplateName].isDefault) {
                this.toast.error(
                    null,
                    'ADMIN.BROWSER.LUCENE_TEMPLATE_MEMORY.DEFAULT_TEMPLATE_OVERRIDE',
                );
                return;
            }
            void this.confirmUpdateTemplate(this.newTemplateName).then((isUpdated) => {
                if (isUpdated) {
                    this.closeNewTemplateDialog();
                }
            });
        } else {
            this.templates[this.newTemplateName] = {
                query: this.query,
                properties: this.properties,
            };
            this.updateStorage();
            this.selectedTemplate = this.newTemplateName;
            this.closeNewTemplateDialog();
        }
    }

    closeNewTemplateDialog(): void {
        this.isNewTemplateDialogVisible = false;
        this.newTemplateName = '';
    }

    loadTemplate(key: string) {
        this.selectedTemplate = key;
        this.query = this.templates[key].query;
        this.queryChange.emit(this.query);
        this.properties = this.templates[key].properties;
        this.propertiesChange.emit(this.properties);
    }

    async confirmUpdateTemplate(template: string): Promise<boolean> {
        const dialogRef = await this.dialogs.openGenericDialog({
            title: 'ADMIN.BROWSER.LUCENE_TEMPLATE_MEMORY.CONFIRM_UPDATE_TITLE',
            message: 'ADMIN.BROWSER.LUCENE_TEMPLATE_MEMORY.CONFIRM_UPDATE_MESSAGE',
            messageParameters: { template },
            buttons: [
                { label: 'CANCEL', config: { color: 'standard' } },
                {
                    label: 'ADMIN.BROWSER.LUCENE_TEMPLATE_MEMORY.CONFIRM_UPDATE_BUTTON',
                    config: { color: 'primary' },
                },
            ],
        });
        const response = await dialogRef.afterClosed().toPromise();
        if (response === 'ADMIN.BROWSER.LUCENE_TEMPLATE_MEMORY.CONFIRM_UPDATE_BUTTON') {
            this.updateTemplate(template);
            return true;
        } else {
            return false;
        }
    }

    async confirmDeleteTemplate(template: string): Promise<void> {
        const dialogRef = await this.dialogs.openGenericDialog({
            title: 'ADMIN.BROWSER.LUCENE_TEMPLATE_MEMORY.CONFIRM_DELETE_TITLE',
            message: 'ADMIN.BROWSER.LUCENE_TEMPLATE_MEMORY.CONFIRM_DELETE_MESSAGE',
            messageParameters: { template },
            buttons: [
                { label: 'CANCEL', config: { color: 'standard' } },
                {
                    label: 'ADMIN.BROWSER.LUCENE_TEMPLATE_MEMORY.CONFIRM_DELETE_BUTTON',
                    config: { color: 'danger' },
                },
            ],
        });
        dialogRef.afterClosed().subscribe((response) => {
            if (response === 'ADMIN.BROWSER.LUCENE_TEMPLATE_MEMORY.CONFIRM_DELETE_BUTTON') {
                this.deleteTemplate(template);
            }
        });
    }

    private updateTemplate(template: string): void {
        this.templates[template] = {
            query: this.query,
            properties: this.properties,
        };
        this.updateStorage();
    }

    private deleteTemplate(template: string): void {
        this.selectedTemplate = null;
        delete this.templates[template];
        this.updateStorage();
    }

    private updateStorage() {
        const storeTemplates: LuceneTemplates = {};
        Object.keys(this.templates)
            .filter((t) => !this.templates[t].isDefault)
            .forEach((t) => (storeTemplates[t] = this.templates[t]));
        void this.storage.set(LuceneTemplateMemoryComponent.STORAGE_KEY, storeTemplates);
    }
}
