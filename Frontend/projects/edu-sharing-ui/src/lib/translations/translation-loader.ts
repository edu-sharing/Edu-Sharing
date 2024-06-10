import { HttpClient } from '@angular/common/http';
import { Inject, Optional } from '@angular/core';
import { TranslateLoader } from '@ngx-translate/core';
import { ConfigService, LANGUAGES } from 'ngx-edu-sharing-api';
import * as rxjs from 'rxjs';
import { concat, Observable, of } from 'rxjs';
import { catchError, first, map, reduce } from 'rxjs/operators';
import { EduSharingUiConfiguration } from '../edu-sharing-ui-configuration';
import { ASSETS_BASE_PATH } from '../types/injection-tokens';
import { TranslationSource } from './translation-source';

export const TRANSLATION_LIST = [
    'common',
    'admin',
    'recycle',
    'workspace',
    'search',
    'collections',
    'login',
    'permissions',
    'oer',
    'messages',
    'register',
    'profiles',
    'services',
    'stream',
    'override',
];

type Dictionary = { [key: string]: string | Dictionary };

export class TranslationLoader implements TranslateLoader {
    static create(
        http: HttpClient,
        configService: ConfigService,
        configuration: EduSharingUiConfiguration,
        @Optional() @Inject(ASSETS_BASE_PATH) assetsBasePath?: string,
    ) {
        return new TranslationLoader(assetsBasePath, http, configService, configuration);
    }

    private constructor(
        @Optional() @Inject(ASSETS_BASE_PATH) private assetsBasePath: string,
        private http: HttpClient,
        private configService: ConfigService,
        private configuration: EduSharingUiConfiguration,
        private prefix: string = (assetsBasePath ?? '') + 'assets/i18n',
        private suffix: string = '.json',
    ) {}

    // If you need to configure this, define an injectable configuration object. See
    // https://angular.io/guide/dependency-injection-providers#injecting-a-configuration-object.
    private readonly source: TranslationSource = TranslationSource.Auto;

    /**
     * Gets the translations from the server
     */
    getTranslation(lang: string): Observable<Dictionary> {
        if (lang === 'none') {
            return of({});
        }
        if (!LANGUAGES[lang]) {
            console.error('unknown locale for language ' + lang);
        }
        this.configService.setLocale(LANGUAGES[lang]);
        return rxjs
            .forkJoin({
                originalTranslations: this.getOriginalTranslations(lang).pipe(
                    // Default to empty dictionary if we got nothing
                    map((translations) => translations || {}),
                ),
                translationOverrides: this.configService
                    .observeTranslationOverrides()
                    .pipe(first()),
            })
            .pipe(
                map(({ originalTranslations, translationOverrides }) =>
                    // FIXME: This will alter the object returned by `getOriginalTranslations`.
                    this.applyOverrides(originalTranslations, translationOverrides),
                ),
                map((translations) => this.replaceGenderCharacter(translations)),
                catchError((error, obs) => {
                    console.error(error);
                    return of(error);
                }),
            );
    }

    private getOriginalTranslations(lang: string): Observable<Dictionary> {
        switch (this.getSource()) {
            case 'repository':
                return this.configService.observeDefaultTranslations().pipe(first());
            case 'local':
                return this.mergeTranslations(this.fetchTranslations(lang));
        }
    }

    private getSource(): 'repository' | 'local' {
        if (
            (this.configuration.production && this.source === TranslationSource.Auto) ||
            this.source === TranslationSource.Repository
        ) {
            return 'repository';
        } else {
            return 'local';
        }
    }

    /**
     * Returns an array of Observables that will each fetch a translations json
     * file.
     */
    private fetchTranslations(lang: string): Observable<Dictionary>[] {
        return TRANSLATION_LIST.map(
            (translation) => `${this.prefix}/${translation}/${lang}${this.suffix}`,
        )
            .map((url) => this.http.get(url) as Observable<Dictionary>)
            .map((obs) =>
                obs.pipe(
                    catchError((e) => {
                        console.warn(
                            'missing translation file for language ' +
                                lang +
                                ', translations will be missing / falling back to the default language',
                        );
                        return of({});
                    }),
                ),
            );
    }

    /**
     * Takes an array as returned by `fetchTranslations` and converts it to an
     * Observable that yields a single Dictionary object.
     */
    private mergeTranslations(translations: Observable<Dictionary>[]): Observable<Dictionary> {
        return concat(...translations).pipe(
            reduce((acc: Dictionary, value: Dictionary) => {
                for (const prop in value) {
                    if (value.hasOwnProperty(prop)) {
                        acc[prop] = value[prop];
                    }
                }
                return acc;
            }, {}),
        );
    }

    /**
     * Applies `overrides` to `translations` and returns `translations`.
     *
     * Example:
     *  translations = { foo: { bar: 'bar' } }
     *  overrides = { 'foo.bar': 'baz' }
     * results in
     *  translations = { foo: {bar: 'baz' } }
     *
     * @param translations Nested translations object.
     * @param overrides Flat object with dots (.) in keys interpreted as
     * separators.
     */
    private applyOverrides(
        translations: Dictionary,
        overrides: { [key: string]: string },
    ): Dictionary {
        if (overrides) {
            for (const [key, value] of Object.entries<string>(overrides)) {
                let ref = translations;
                const path = key.split('.');
                const pathLast = path.pop();
                for (const item of path) {
                    if (!ref[item]) {
                        ref[item] = {};
                    }
                    const refItem = ref[item];
                    if (typeof refItem === 'string') {
                        throw new Error('Trying to override leave with sub tree: ' + path);
                    }
                    ref = refItem;
                }
                ref[pathLast] = value;
            }
        }
        return translations;
    }

    private replaceGenderCharacter(translations: Dictionary, path: string[] = []) {
        for (let key of Object.keys(translations)) {
            if (typeof translations[key] === 'string') {
                // DO NOT REMOVE (required for csv language dumping)
                /*console.log(CsvHelper.fromArray(null, [[
                        path.concat(key).join('.'), translations[key]
                ]]));*/
                translations[key] = (translations[key] as string).replace(
                    /{{GENDER_SEPARATOR}}/g,
                    '*',
                );
            } else {
                translations[key] = this.replaceGenderCharacter(
                    translations[key] as Dictionary,
                    path.concat(key),
                );
            }
        }

        return translations;
    }
}
