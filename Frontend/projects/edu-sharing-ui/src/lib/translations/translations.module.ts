import { HttpClient } from '@angular/common/http';
import { ModuleWithProviders, NgModule, Optional, SkipSelf } from '@angular/core';
import { MissingTranslationHandler, TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { ConfigService } from 'ngx-edu-sharing-api';
import { EduSharingUiConfiguration } from '../edu-sharing-ui-configuration';
import { ADDITIONAL_I18N_PROVIDER, ASSETS_BASE_PATH } from '../types/injection-tokens';
import { FallbackTranslationHandler } from './fallback-translation-handler';
import { TranslationLoader } from './translation-loader';

/**
 * Import this module once in the app module to provide the `TranslateService`.
 *
 * Export `TranslateModule` in the shared module to provide directives and pipes.
 */
@NgModule({
    declarations: [],
    imports: [
        TranslateModule.forRoot({
            loader: {
                provide: TranslateLoader,
                useFactory: TranslationLoader.create,
                deps: [
                    HttpClient,
                    ConfigService,
                    EduSharingUiConfiguration,
                    [new Optional(), ASSETS_BASE_PATH],
                    [new Optional(), ADDITIONAL_I18N_PROVIDER],
                ],
            },
            missingTranslationHandler: {
                provide: MissingTranslationHandler,
                useClass: FallbackTranslationHandler,
            },
        }),
    ],
})
export class TranslationsModule {
    static forRoot(): ModuleWithProviders<TranslationsModule> {
        return {
            ngModule: TranslationsModule,
            providers: [],
        };
    }

    constructor(@Optional() @SkipSelf() parentModule?: TranslationsModule) {
        if (parentModule) {
            console.warn(
                'TranslationsModule is already loaded. Import it in the AppModule only',
                parentModule,
            );
            /*throw new Error(
                'TranslationsModule is already loaded. Import it in the AppModule only',
            );*/
        }
    }
}
