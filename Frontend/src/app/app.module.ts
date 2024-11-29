import { DragDropModule } from '@angular/cdk/drag-drop';
import { LocationStrategy } from '@angular/common';
import { NgModule } from '@angular/core';
import { MAT_FORM_FIELD_DEFAULT_OPTIONS } from '@angular/material/form-field';
import { MAT_TOOLTIP_DEFAULT_OPTIONS, MatTooltipDefaultOptions } from '@angular/material/tooltip';
import { MaterialCssVarsModule } from 'angular-material-css-vars';
import { ResizableModule } from 'angular-resizable-element';
import {
    EDU_SHARING_API_CONFIG,
    EduSharingApiConfigurationParams,
    EduSharingApiModule,
} from 'ngx-edu-sharing-api';
import { CoreUiModule } from './core-ui-module/core-ui.module';
import { ErrorHandlerService } from './core-ui-module/error-handler.service';
import { DECLARATIONS } from './declarations';
import { extensionDeclarations } from './extension/extension-declarations';
import { extensionImports } from './extension/extension-imports';
import { extensionProviders } from './extension/extension-providers';
import { extensionSchemas } from './extension/extension-schemas';
import { DialogsModule } from './features/dialogs/dialogs.module';
import { MdsModule } from './features/mds/mds.module';
import {
    AppService as AppServiceAbstract,
    EduSharingUiModule,
    KeyboardShortcutsService as KeyboardShortcutsServiceAbstract,
    OptionsHelperService as OptionsHelperServiceAbstract,
    Toast as ToastAbstract,
    TranslationsModule,
} from 'ngx-edu-sharing-ui';
import { IMPORTS } from './imports';
import { MainModule } from './main/main.module';
import { DECLARATIONS_ADMIN } from './modules/admin/declarations';
import { LtiAdminComponent } from './modules/admin/lti-admin/lti-admin.component';
import { LtitoolAdminComponent } from './modules/admin/ltitool-admin/ltitool-admin.component';
import { LuceneTemplateMemoryComponent } from './modules/admin/lucene-template-memory/lucene-template-memory.component';
import { DECLARATIONS_COLLECTIONS } from './modules/collections/declarations';
import { DECLARATIONS_FILE_UPLOAD } from './modules/file-upload/declarations';
import { DECLARATIONS_LOGINAPP } from './modules/login-app/declarations';
import { DECLARATIONS_LOGIN } from './modules/login/declarations';
import { LtiComponent } from './modules/lti/lti.component';
import { DECLARATIONS_MANAGEMENT_DIALOGS } from './modules/management-dialogs/declarations';
import { DECLARATIONS_MESSAGES } from './modules/messages/declarations';
import { DECLARATIONS_RECYCLE } from './modules/node-list/declarations';
import { DECLARATIONS_OER } from './modules/oer/declarations';
import { DECLARATIONS_PERMISSIONS } from './modules/permissions/declarations';
import { DECLARATIONS_PROFILES } from './modules/profiles/declarations';
import { DECLARATIONS_REGISTER } from './modules/register/declarations';
import { DECLARATIONS_SERVICES } from './modules/services/declarations';
import { DECLARATIONS_SHARE_APP } from './modules/share-app/declarations';
import { DECLARATIONS_SHARING } from './modules/sharing/declarations';
import { DECLARATIONS_STARTUP } from './modules/startup/declarations';
import { DECLARATIONS_STREAM } from './modules/stream/declarations';
import { DECLARATIONS_WORKSPACE } from './modules/workspace/declarations';
import { PROVIDERS } from './providers';
import { AppLocationStrategy } from './router/location-strategy';
import { RouterComponent } from './router/router.component';
import { SharedModule } from './shared/shared.module';
import { environment } from '../environments/environment';
import { Toast } from './core-ui-module/toast';
import { OptionsHelperService } from './core-ui-module/options-helper.service';
import { KeyboardShortcutsService } from './services/keyboard-shortcuts.service';
import { CordovaService } from './common/services/cordova.service';
import { InMemoryCache } from '@apollo/client/core';
import { HttpLink } from 'apollo-angular/http';
import { APOLLO_OPTIONS, ApolloModule } from 'apollo-angular';
import { EduSharingGraphqlModule } from 'ngx-edu-sharing-graphql';
import { MAT_CHECKBOX_DEFAULT_OPTIONS } from '@angular/material/checkbox';
import { MAT_RADIO_DEFAULT_OPTIONS } from '@angular/material/radio';
import { MAT_SLIDE_TOGGLE_DEFAULT_OPTIONS } from '@angular/material/slide-toggle';
import { FooterComponent } from './common/ui/footer/footer.component';

// http://blog.angular-university.io/angular2-ngmodule/
// -> Making modules more readable using the spread operator

const matTooltipDefaultOptions: MatTooltipDefaultOptions = {
    showDelay: 500,
    hideDelay: 0,
    touchendHideDelay: 0,
};

@NgModule({
    declarations: [
        DECLARATIONS,
        DECLARATIONS_RECYCLE,
        DECLARATIONS_WORKSPACE,
        DECLARATIONS_COLLECTIONS,
        DECLARATIONS_LOGIN,
        DECLARATIONS_REGISTER,
        DECLARATIONS_LOGINAPP,
        DECLARATIONS_FILE_UPLOAD,
        DECLARATIONS_STARTUP,
        DECLARATIONS_PERMISSIONS,
        DECLARATIONS_OER,
        DECLARATIONS_STREAM,
        DECLARATIONS_MANAGEMENT_DIALOGS,
        DECLARATIONS_ADMIN,
        DECLARATIONS_PROFILES,
        DECLARATIONS_MESSAGES,
        DECLARATIONS_SHARING,
        DECLARATIONS_SHARE_APP,
        DECLARATIONS_SERVICES,
        LuceneTemplateMemoryComponent,
        extensionDeclarations,
        LtiComponent,
        LtiAdminComponent,
        LtitoolAdminComponent,
    ],
    imports: [
        IMPORTS,
        FooterComponent,
        ApolloModule,
        SharedModule,
        MainModule,
        EduSharingApiModule.forRoot(),
        EduSharingUiModule.forRoot({ production: environment.production }),
        TranslationsModule.forRoot(),
        EduSharingGraphqlModule,
        DragDropModule,
        extensionImports,
        ResizableModule,
        MdsModule,
        DialogsModule,
        CoreUiModule,
        MaterialCssVarsModule.forRoot({
            isAutoContrast: true,
        }),
    ],
    providers: [
        { provide: ToastAbstract, useClass: Toast },
        { provide: OptionsHelperServiceAbstract, useClass: OptionsHelperService },
        { provide: KeyboardShortcutsServiceAbstract, useClass: KeyboardShortcutsService },
        { provide: CordovaService, useClass: AppServiceAbstract },
        {
            provide: EDU_SHARING_API_CONFIG,
            deps: [ErrorHandlerService],
            useFactory: (errorHandler: ErrorHandlerService) =>
                ({
                    onError: (err, req) => errorHandler.handleError(err, req),
                } as EduSharingApiConfigurationParams),
        },
        {
            provide: APOLLO_OPTIONS,
            useFactory: (httpLink: HttpLink) => {
                return {
                    link: httpLink.create({ uri: '/edu-sharing/graphql' }),
                    cache: new InMemoryCache(),
                };
            },
            deps: [HttpLink],
        },
        { provide: LocationStrategy, useClass: AppLocationStrategy },
        PROVIDERS,
        { provide: MAT_FORM_FIELD_DEFAULT_OPTIONS, useValue: { appearance: 'outline' } },
        { provide: MAT_TOOLTIP_DEFAULT_OPTIONS, useValue: matTooltipDefaultOptions },
        { provide: MAT_CHECKBOX_DEFAULT_OPTIONS, useValue: { color: 'primary' } },
        { provide: MAT_RADIO_DEFAULT_OPTIONS, useValue: { color: 'primary' } },
        { provide: MAT_SLIDE_TOGGLE_DEFAULT_OPTIONS, useValue: { color: 'primary' } },
        extensionProviders,
        ErrorHandlerService,
    ],
    exports: [
        DECLARATIONS,
        DECLARATIONS_RECYCLE,
        DECLARATIONS_WORKSPACE,
        DECLARATIONS_COLLECTIONS,
        DECLARATIONS_LOGIN,
        DECLARATIONS_REGISTER,
        DECLARATIONS_LOGINAPP,
        DECLARATIONS_FILE_UPLOAD,
        DECLARATIONS_STARTUP,
        DECLARATIONS_PERMISSIONS,
        DECLARATIONS_OER,
        DECLARATIONS_STREAM,
        DECLARATIONS_MANAGEMENT_DIALOGS,
        DECLARATIONS_ADMIN,
        DECLARATIONS_PROFILES,
        DECLARATIONS_MESSAGES,
        DECLARATIONS_SHARING,
        DECLARATIONS_SHARE_APP,
        DECLARATIONS_SERVICES,
    ],
    schemas: [].concat(extensionSchemas),
    bootstrap: [RouterComponent],
})
export class AppModule {}
