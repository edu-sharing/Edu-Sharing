import { HttpClient } from '@angular/common/http';
import { fakeAsync, tick } from '@angular/core/testing';
import { ConfigService, LANGUAGES, Locale, TranslationsDict } from 'ngx-edu-sharing-api';
import * as rxjs from 'rxjs';
import { Observable } from 'rxjs';
import { TRANSLATION_LIST, TranslationLoader } from './translation-loader';
import { TranslationSource } from './translation-source';
import { EduSharingUiConfiguration } from '../edu-sharing-ui-configuration';

class HttpClientStub {
    get(url: string): Observable<any> {
        return rxjs.of(null);
    }
}
class EduSharingUiConfigurationStub {
    production = false;
}
class ConfigStub {
    observeTranslationOverrides(lang: string): Observable<TranslationsDict> {
        return rxjs.of(null);
    }
    observeDefaultTranslations(lang: string): Observable<{
        locale: Locale;
        dict: Observable<TranslationsDict>;
    }> {
        return rxjs.of(null);
    }
    setLocale(lang: string): void {}
}

describe('TranslationLoader', () => {
    let translationLoader: TranslationLoader;
    let httpClient: HttpClientStub;
    let config: ConfigStub;
    let uiConfig: EduSharingUiConfigurationStub;

    beforeEach(() => {
        httpClient = new HttpClientStub();
        config = new ConfigStub();
        uiConfig = new EduSharingUiConfigurationStub();
        translationLoader = TranslationLoader.create(
            httpClient as HttpClient,
            config as unknown as ConfigService,
            uiConfig as unknown as EduSharingUiConfiguration,
        );
    });

    describe('getTranslation', () => {
        function callGetTranslation(lang: string) {
            return new Promise((resolve) => {
                fakeAsync(() => {
                    translationLoader
                        .getTranslation(lang)
                        .subscribe((result) => resolve(result), fail);
                    tick(100);
                })();
            });
        }

        it('should run', async () => {
            const result = await callGetTranslation('de');
            expect(result).toEqual({});
        });

        describe('Source = Local', () => {
            beforeEach(() => {
                (translationLoader as any).source = TranslationSource.Local;
            });

            it('should call nothing for lang=none', async () => {
                const getSpy = spyOn(httpClient, 'get').and.callThrough();
                const getConfigLanguageSpy = spyOn(
                    config,
                    'observeTranslationOverrides',
                ).and.callThrough();
                const getLanguageDefaultsSpy = spyOn(
                    config,
                    'observeDefaultTranslations',
                ).and.callThrough();
                await callGetTranslation('none');
                expect(getSpy.calls.count()).toBe(0);
                expect(getConfigLanguageSpy.calls.count()).toBe(0);
                expect(getLanguageDefaultsSpy.calls.count()).toBe(0);
            });

            it('should call http get', async () => {
                const getSpy = spyOn(httpClient, 'get').and.callThrough();
                await callGetTranslation('de');
                expect(getSpy.calls.count()).toBe(TRANSLATION_LIST.length);
                expect(getSpy.calls.first().args).toEqual(['assets/i18n/common/de.json']);
                expect(getSpy.calls.mostRecent().args).toEqual(['assets/i18n/override/de.json']);
            });

            it('should call observeTranslationOverrides', async () => {
                const getConfigLanguageSpy = spyOn(
                    config,
                    'observeTranslationOverrides',
                ).and.callThrough();
                await callGetTranslation('de');
                expect(getConfigLanguageSpy.calls.count()).toBe(1);
            });

            it('should call setLocale with correct locale', async () => {
                const setLocaleSpy = spyOn(config, 'setLocale').and.callThrough();
                await callGetTranslation('de');
                expect(setLocaleSpy.calls.mostRecent().args).toEqual(['de_DE']);
            });

            it('should not call observeDefaultTranslations', async () => {
                const getLanguageDefaultsSpy = spyOn(
                    config,
                    'observeDefaultTranslations',
                ).and.callThrough();
                await callGetTranslation('de');
                expect(getLanguageDefaultsSpy.calls.count()).toBe(0);
            });

            it('should include translations via http', async () => {
                httpClient.get = (url) => {
                    if (url === 'assets/i18n/common/de.json') {
                        return rxjs.of({ foo: 'bar' });
                    }
                    return rxjs.of(null);
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ foo: 'bar' });
            });

            it('should merge translations via http', async () => {
                httpClient.get = (url) => {
                    if (url === 'assets/i18n/common/de.json') {
                        return rxjs.of({ foo: 'bar' });
                    } else if (url === 'assets/i18n/admin/de.json') {
                        return rxjs.of({ bar: 'baz' });
                    }
                    return rxjs.of(null);
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ foo: 'bar', bar: 'baz' });
            });

            it('should override nested translations via http', async () => {
                httpClient.get = (url) => {
                    if (url === 'assets/i18n/common/de.json') {
                        return rxjs.of({ prefix: { foo: 'bar' } });
                    } else if (url === 'assets/i18n/admin/de.json') {
                        return rxjs.of({ prefix: { bar: 'baz' } });
                    }
                    return rxjs.of(null);
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ prefix: { bar: 'baz' } });
            });

            it('should include translations via observeTranslationOverrides', async () => {
                config.observeTranslationOverrides = (lang) => {
                    return rxjs.of({ foo: 'bar' });
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ foo: 'bar' });
            });

            it('should override translations via observeTranslationOverrides', async () => {
                httpClient.get = (url) => {
                    if (url === 'assets/i18n/common/de.json') {
                        return rxjs.of({ foo: 'bar' });
                    }
                    return rxjs.of(null);
                };
                config.observeTranslationOverrides = (lang) => {
                    return rxjs.of({ foo: 'baz' });
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ foo: 'baz' });
            });

            it('should merge translations via observeTranslationOverrides', async () => {
                httpClient.get = (url) => {
                    if (url === 'assets/i18n/common/de.json') {
                        return rxjs.of({ foo: 'bar' });
                    }
                    return rxjs.of(null);
                };
                config.observeTranslationOverrides = (lang) => {
                    return rxjs.of({ bar: 'baz' });
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ foo: 'bar', bar: 'baz' });
            });

            it('should override nested translations via observeTranslationOverrides', async () => {
                httpClient.get = (url) => {
                    if (url === 'assets/i18n/common/de.json') {
                        return rxjs.of({ prefix: { foo: 'bar' } });
                    }
                    return rxjs.of(null);
                };
                config.observeTranslationOverrides = (lang) => {
                    return rxjs.of({ prefix: { bar: 'baz' } });
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ prefix: { bar: 'baz' } });
            });

            it('should deep-merge translations via observeTranslationOverrides', async () => {
                httpClient.get = (url) => {
                    if (url === 'assets/i18n/common/de.json') {
                        return rxjs.of({ prefix: { foo: 'bar' } });
                    }
                    return rxjs.of(null);
                };
                config.observeTranslationOverrides = (lang) => {
                    return rxjs.of({ 'prefix.bar': 'baz' });
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ prefix: { foo: 'bar', bar: 'baz' } });
            });

            it('should deep-merge translations via observeTranslationOverrides (3 levels)', async () => {
                httpClient.get = (url) => {
                    if (url === 'assets/i18n/common/de.json') {
                        return rxjs.of({
                            l1: { l2: { l3: { foo: 'bar' } } },
                        });
                    }
                    return rxjs.of(null);
                };
                config.observeTranslationOverrides = (lang) => {
                    return rxjs.of({ 'l1.l2.l3.bar': 'baz' });
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({
                    l1: { l2: { l3: { foo: 'bar', bar: 'baz' } } },
                });
            });

            it('should create missing levels via observeTranslationOverrides', async () => {
                httpClient.get = (url) => {
                    if (url === 'assets/i18n/common/de.json') {
                        return rxjs.of({});
                    }
                    return rxjs.of(null);
                };
                config.observeTranslationOverrides = (lang) => {
                    return rxjs.of({ 'l1.l2.l3.bar': 'baz' });
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({
                    l1: { l2: { l3: { bar: 'baz' } } },
                });
            });
        });

        describe('Source = Repository', () => {
            beforeEach(() => {
                (translationLoader as any).source = TranslationSource.Repository;
            });

            it('should call nothing for lang=none', async () => {
                const getSpy = spyOn(httpClient, 'get').and.callThrough();
                const getConfigLanguageSpy = spyOn(
                    config,
                    'observeTranslationOverrides',
                ).and.callThrough();
                const getLanguageDefaultsSpy = spyOn(
                    config,
                    'observeDefaultTranslations',
                ).and.callThrough();
                await callGetTranslation('none');
                expect(getSpy.calls.count()).toBe(0);
                expect(getConfigLanguageSpy.calls.count()).toBe(0);
                expect(getLanguageDefaultsSpy.calls.count()).toBe(0);
            });

            it('should not call http get', async () => {
                const getSpy = spyOn(httpClient, 'get').and.callThrough();
                await callGetTranslation('de');
                expect(getSpy.calls.count()).toBe(0);
            });

            it('should call observeTranslationOverrides', async () => {
                const getConfigLanguageSpy = spyOn(
                    config,
                    'observeTranslationOverrides',
                ).and.callThrough();
                await callGetTranslation('de');
                expect(getConfigLanguageSpy.calls.count()).toBe(1);
            });

            it('should call observeDefaultTranslations', async () => {
                const getLanguageDefaultsSpy = spyOn(
                    config,
                    'observeDefaultTranslations',
                ).and.callThrough();
                await callGetTranslation('de');
                expect(getLanguageDefaultsSpy.calls.count()).toBe(1);
            });

            it('should call setLocale with correct locale', async () => {
                const setLocaleSpy = spyOn(config, 'setLocale').and.callThrough();
                await callGetTranslation('de');
                expect(setLocaleSpy.calls.mostRecent().args).toEqual(['de_DE']);
            });

            it('should include translations via observeDefaultTranslations', async () => {
                config.observeDefaultTranslations = (lang) =>
                    rxjs.of({ locale: LANGUAGES['de'], dict: rxjs.of({ foo: 'bar' }) });
                const result = await callGetTranslation('de');
                expect(result).toEqual({ foo: 'bar' });
            });

            it('should include translations via observeTranslationOverrides', async () => {
                config.observeTranslationOverrides = (lang) => {
                    return rxjs.of({ foo: 'bar' });
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ foo: 'bar' });
            });

            it('should merge translations via observeTranslationOverrides', async () => {
                config.observeDefaultTranslations = (lang) =>
                    rxjs.of({ locale: LANGUAGES['de'], dict: rxjs.of({ foo: 'bar' }) });
                config.observeTranslationOverrides = (lang) => {
                    return rxjs.of({ bar: 'baz' });
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ foo: 'bar', bar: 'baz' });
            });

            it('should override nested translations via observeTranslationOverrides', async () => {
                config.observeDefaultTranslations = (lang) =>
                    rxjs.of({ locale: LANGUAGES['de'], dict: rxjs.of({ prefix: { foo: 'bar' } }) });
                config.observeTranslationOverrides = (lang) => {
                    return rxjs.of({ prefix: { bar: 'baz' } });
                };
                const result = await callGetTranslation('de');
                expect(result).toEqual({ prefix: { bar: 'baz' } });
            });

            it('should deep-merge translations via observeTranslationOverrides', async () => {
                config.observeDefaultTranslations = (lang) =>
                    rxjs.of({ locale: LANGUAGES['de'], dict: rxjs.of({ prefix: { foo: 'bar' } }) });
                config.observeTranslationOverrides = (lang) => rxjs.of({ 'prefix.bar': 'baz' });
                const result = await callGetTranslation('de');
                expect(result).toEqual({ prefix: { foo: 'bar', bar: 'baz' } });
            });
        });
    });
});
