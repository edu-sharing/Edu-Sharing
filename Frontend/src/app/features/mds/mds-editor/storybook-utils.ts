import { ApplicationConfig } from '@angular/core';
import { MAT_FORM_FIELD_DEFAULT_OPTIONS } from '@angular/material/form-field';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideAnimations } from '@angular/platform-browser/animations';
import { TranslateService } from '@ngx-translate/core';
import { MdsWidget, SuggestionResponseDto } from 'ngx-edu-sharing-api';
import { BehaviorSubject, Observable, Subject, of } from 'rxjs';
import { CordovaService } from '../../../services/cordova.service';
import { Toast } from '../../../services/toast';
import { InputStatus, MdsWidgetValue } from '../types/types';
import { InitialValues, MdsEditorInstanceService } from './mds-editor-instance.service';
import { ViewInstanceService } from './mds-editor-view/view-instance.service';

export const translateProvider = {
    instant: (v: string) => v,
    get: (v: string) => of(v),
    onTranslationChange: of({ lang: 'none' }),
    onDefaultLangChange: of({ lang: 'none' }),
    onLangChange: of({}),
};
export const mdsStorybookProviders: ApplicationConfig['providers'] = [
    { provide: MAT_FORM_FIELD_DEFAULT_OPTIONS, useValue: { appearance: 'outline' } },
    MdsEditorInstanceService,
    ViewInstanceService,
    CordovaService,
    Toast,
    {
        provide: TranslateService,
        useValue: translateProvider,
    },
    MatSnackBar,
    provideAnimations(),
];

export class WidgetDummy {
    readonly focusTrigger = new Subject<void>();
    readonly status = new BehaviorSubject<InputStatus>(null);
    readonly meetsDynamicCondition = new BehaviorSubject<boolean>(true);
    readonly defaultDefinition: Partial<MdsWidget> = {};

    public constructor(public definition: MdsWidget) {
        this.definition = { ...this.defaultDefinition, ...this.definition };
    }

    getInitalValuesAsync(): Promise<InitialValues> {
        return Promise.resolve(this.getInitialValues());
    }

    getInitialValues(): InitialValues {
        return {
            individualValues: [],
            jointValues: [],
        };
    }

    observeIsDisabled(): Observable<boolean> {
        return of(false);
    }

    public getInternalError(): string {
        return '';
    }

    setStatus(value: InputStatus): void {
        console.log(value);
        this.status.next(value);
    }

    getStatus(): InputStatus {
        return this.status.value;
    }

    registerShowMissingRequired() {}

    setValue() {}

    getSuggestions(): SuggestionResponseDto[] {
        return [
            {
                value: 'hello world',
                createdBy: {
                    authorityName: 'Superschlauer Duper KI',
                },
                status: 'PENDING',
            },
            {
                value: 'AB\nDEF',
                createdBy: {
                    authorityName: 'Noch schlauerer Redakteur',
                },
                status: 'PENDING',
            },
        ];
    }
}
