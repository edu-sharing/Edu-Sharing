import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of, Subject } from 'rxjs';
import {
    distinctUntilChanged,
    filter,
    map,
    shareReplay,
    startWith,
    switchMap,
    take,
    tap,
} from 'rxjs/operators';
import { ApiRequestConfiguration } from '../api-request-configuration';
import * as apiModels from '../api/models';
import { ConfigV1Service } from '../api/services';
import { switchReplay } from '../utils/rxjs-operators/switch-replay';

export type ClientConfig = apiModels.Values;
export type Variables = apiModels.Variables['current'];
export type TranslationsDict = { [key: string]: string | TranslationsDict };

export type Locale = 'de_DE' | 'en_US' | 'fr_FR' | 'it_IT';
export const LANGUAGES: { [key: string]: Locale } = {
    de: 'de_DE',
    en: 'en_US',
    fr: 'fr_FR',
    it: 'it_IT',
};

/**
 * Provides system configuration.
 */
@Injectable({
    providedIn: 'root',
})
export class ConfigService {
    private readonly updateTrigger = new Subject<void>();
    private readonly localeSubject = new BehaviorSubject<Locale | null>(null);
    private configSubject = new BehaviorSubject<apiModels.Values | undefined>(undefined);
    private readonly config$ = this.updateTrigger.pipe(
        startWith(void 0 as void),
        switchReplay(() => this.configV1.getConfig1()),
        tap((config) => this.configSubject.next(config.current)),
        map((config) => (config.current ? config : null)),
    );
    private readonly variables$ = this.updateTrigger.pipe(
        startWith(void 0 as void),
        switchReplay(() => this.configV1.getVariables()),
        map((variables) => variables.current ?? null),
    );
    private readonly defaultTranslations$ = this.localeSubject.pipe(
        filter((locale) => locale !== null),
        distinctUntilChanged(),
        switchMap((locale) => of({ locale: locale, dict: this.configV1.getLanguageDefaults() })),
        shareReplay(1),
    ) as unknown as Observable<{ locale: Locale; dict: Observable<TranslationsDict> }>;
    private readonly translationOverrides$ = this.localeSubject.pipe(
        filter((locale) => locale !== null),
        distinctUntilChanged(),
        switchMap(() => this.configV1.getLanguage()),
        map((language) => language.current ?? null),
        shareReplay(1),
    );

    constructor(
        private configV1: ConfigV1Service,
        private apiRequestConfiguration: ApiRequestConfiguration,
    ) {}

    /**
     * Returns the current system configuration.
     *
     * The configuration might depend on the domain via which the application is served.
     *
     * The observable will update on changes.
     */
    observeConfig({ forceUpdate = false } = {}): Observable<ClientConfig | null> {
        if (forceUpdate) {
            this.updateTrigger.next();
        }
        return this.config$.pipe(map((c) => c?.current ?? null));
    }

    observeContextId(): Observable<string | null> {
        return this.config$.pipe(map((c) => c?.contextId ?? null));
    }

    /**
     * Returns variables defined in the client configuration.
     *
     * The observable will update on changes.
     */
    observeVariables(): Observable<Variables | null> {
        return this.variables$;
    }

    /**
     * Sets the locale for future API requests.
     *
     * This affects translations and localized MDS widget values.
     */
    setLocale(locale: Locale): void {
        if (locale) {
            this.apiRequestConfiguration.setLocale(locale);
            this.localeSubject.next(locale);
        } else {
            console.warn('Called `setLocale` with undefined `locale`');
        }
    }

    /**
     * Returns accumulated default translations.
     *
     * The observable will update on locale changes.
     *
     * @returns a nested dictionary of default translations
     */
    observeDefaultTranslations(): Observable<{
        locale: Locale;
        dict: Observable<TranslationsDict>;
    }> {
        return this.defaultTranslations$;
    }

    /**
     * Returns translation overrides.
     *
     * The observable will update on locale changes.
     *
     * @returns a flat dictionary of translation overrides, key parts separated by "."
     */
    observeTranslationOverrides(): Observable<{ [key: string]: string } | null> {
        return this.translationOverrides$;
    }

    private instantInternal<T>(
        name: string,
        defaultValue?: T,
        object: any = this.configSubject.value,
    ): T {
        if (!object) return defaultValue as T;
        let parts = name.split('.');
        if (parts.length > 1) {
            if (object[parts[0]]) {
                let joined = name.substr(parts[0].length + 1);
                return this.instantInternal(joined, defaultValue, object[parts[0]]);
            } else {
                return defaultValue as T;
            }
        }
        if (object[name] != null) return object[name];
        return defaultValue as T;
    }
    /**
     * @deprecated
     * Like `get`, but assumes that the configuration is already initialized.
     *
     * It is the responsibility of the caller to assure that the configuration is initialized! If you
     * are not sure, use `get` instead.
     */
    public instant<T = string>(name: string, defaultValue?: T): T {
        return this.instantInternal(name, defaultValue);
    }
    public async get<T = string>(name: string, defaultValue?: T): Promise<T> {
        await this.observeConfig().pipe(take(1)).toPromise();
        return this.instantInternal(name, defaultValue);
    }
}
