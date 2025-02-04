import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import {
    AboutService,
    AdminV1Service,
    ConfigService,
    ConfigV1Service,
    Context,
} from 'ngx-edu-sharing-api';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { Toast, UIAnimation } from 'ngx-edu-sharing-ui';
import { trigger } from '@angular/animations';
import { DialogsService } from '../../../features/dialogs/dialogs.service';
import {
    DELETE_OR_CANCEL,
    YES_OR_NO,
} from '../../../features/dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { first } from 'rxjs/operators';
import { Closable } from '../../../features/dialogs/card-dialog/card-dialog-config';
import { UIService } from '../../../core-module/rest/services/ui.service';
import { ActivatedRoute, Router } from '@angular/router';

type variants = 'variables' | 'Language' | '';
@Component({
    selector: 'es-admin-context-editor',
    templateUrl: 'context.component.html',
    styleUrls: ['context.component.scss'],
    animations: [trigger('openOverlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST))],
})
export class AdminContextComponent implements OnInit {
    readonly ConfigVarians = ['values', 'language', 'variables'];
    @ViewChild('createFormRef') createFormRef: ElementRef;
    context: Context[];
    createForm = this.initForm();
    contextForm: { [key: string]: FormGroup } = {};
    editorOptions: any = {
        minimap: { enabled: false },
        language: 'json',
        automaticLayout: true,
    };
    editorOptionsRO: any = {
        minimap: { enabled: false },
        language: 'json',
        readOnly: true,
        automaticLayout: true,
    };
    private spec: any;
    openCloseEditors: { [key in string]: boolean } = {};
    openCloseContext: { [key in string]: boolean } = {};
    showValues = false;
    loading: boolean;
    currentId: string;
    constructor(
        private configService: ConfigService,
        private router: Router,
        private route: ActivatedRoute,
        private configV1Service: ConfigV1Service,
        private adminV1Service: AdminV1Service,
        private aboutService: AboutService,
        private dialogsService: DialogsService,
        private uiService: UIService,
        private toast: Toast,
    ) {}

    async ngOnInit() {
        this.aboutService.getOpenapiJson().subscribe((api) => {
            console.log(api);
            this.spec = api;
        });
        this.reload();
    }

    async createContext() {
        await this.modifyContext(this.createForm);
    }
    private async modifyContext(form: FormGroup, id?: string) {
        if (!form.valid) {
            this.toast.error(null, 'ADMIN.CONTEXT.MISSING_VALUES');
            return;
        }
        let values, language, variables;
        try {
            values = JSON.parse(form.get('values').value);
            language = JSON.parse(form.get('language').value);
            variables = JSON.parse(form.get('variables').value);
        } catch (e) {
            console.error(e);
            this.toast.error(e, 'ADMIN.CONTEXT.ERROR_PARSING');
            return;
        }
        await this.configV1Service
            .createOrUpdateContext({
                body: {
                    id,
                    domain: form.get('domain').value,
                    values,
                    language,
                    variables,
                },
            })
            .toPromise();
        this.createForm = this.initForm();
        this.toast.toast('ADMIN.CONTEXT.SAVED');
        if (id === this.currentId) {
            const dialogRef = await this.dialogsService.openGenericDialog({
                title: 'ADMIN.CONTEXT.REFRESH_TITLE',
                message: 'ADMIN.CONTEXT.REFRESH_MESSAGE',
                buttons: YES_OR_NO,
                closable: Closable.Casual,
            });
            dialogRef.afterClosed().subscribe(async (response) => {
                if (response === 'YES') {
                    window.location.reload();
                }
            });
        }
        await this.reload();
    }

    private initForm(context: Context = null) {
        return new FormGroup({
            domain: new FormControl(
                context ? context.domain : ([] as string[]),
                Validators.required,
            ),
            values: new FormControl(
                context ? JSON.stringify(context.values, null, 2) : `{}`,
                Validators.required,
            ),
            language: new FormControl(
                context ? JSON.stringify(context.language, null, 2) : `[]`,
                Validators.required,
            ),
            variables: new FormControl(
                context ? JSON.stringify(context.variables, null, 2) : `{}`,
                Validators.required,
            ),
        });
    }

    private async reload() {
        this.loading = true;
        this.currentId = await this.configService.observeContextId().pipe(first()).toPromise();
        this.context = (
            (await this.configV1Service.getAvailableContext({}).toPromise()) as unknown as Context[]
        ).sort((a, b) =>
            a.id === this.currentId
                ? -1
                : b.id === this.currentId
                ? 1
                : a.domain?.[0]?.localeCompare(b.domain?.[0]),
        );
        this.contextForm = {};
        this.context.forEach((c) => {
            this.contextForm[c.id] = this.initForm(c);
        });
        this.loading = false;
    }

    getSpec(values: string) {
        if (values === 'language') {
            return JSON.stringify(
                [
                    {
                        language: 'de',
                        string: [
                            {
                                key: 'EXAMPLE',
                                value: 'value',
                            },
                        ],
                    },
                ],
                null,
                2,
            );
        }
        if (values === 'variables') {
            return JSON.stringify(
                {
                    variable: [
                        {
                            key: 'variable_name',
                            value: 'variable_value',
                        },
                    ],
                },
                null,
                2,
            );
        }
        return JSON.stringify(this.spec?.components?.schemas?.Values?.properties, null, 2);
    }

    async deleteContext(context: Context) {
        const dialogRef = await this.dialogsService.openGenericDialog({
            title: 'ADMIN.CONTEXT.DELETE_TITLE',
            message: 'ADMIN.CONTEXT.DELETE_MESSAGE',
            messageParameters: { id: context.id, domain: context.domain.join(', ') },
            buttons: DELETE_OR_CANCEL,
        });
        dialogRef.afterClosed().subscribe(async (response) => {
            if (response === 'YES_DELETE') {
                await this.configV1Service.deleteContext({ id: context.id }).toPromise();
                this.toast.toast('ADMIN.CONTEXT.DELETED');
                void this.reload();
            }
        });
    }

    async saveContext(c: Context) {
        await this.modifyContext(this.contextForm[c.id], c.id);
    }

    asTemplate(c: Context) {
        this.createForm = this.initForm(c);
        this.uiService.scrollSmooth(0);
    }

    async enableContext(c: Context) {
        await this.adminV1Service
            .enforceContext({
                contextId: c.id,
            })
            .toPromise();
        await this.router.navigate(['./'], {
            relativeTo: this.route,
            queryParams: { skipWarning: true },
            queryParamsHandling: 'merge',
        });
        window.location.reload();
    }
}
