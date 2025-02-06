import { InjectionToken } from '@angular/core';

/**
 * A custom base path for the assets directory.
 *
 * This is useful when the application cannot be served from the base HREF set in index.html. This
 * is the case when it is embedded as a web component into another website.
 */
export const ASSETS_BASE_PATH = new InjectionToken<string>('ASSETS_BASE_PATH');
/**
 * custom additional uris to fetch language files from
 *
 * Might be used if additional angular modules require language data
 *
 * Example
 *{provide: ADDITIONAL_I18N_PROVIDER, useValue: (lang: string) => { return ['/edu-sharing/assets/i18n/myI18n/' + lang + '.json']}},
 */
export const ADDITIONAL_I18N_PROVIDER = new InjectionToken<(lang: string) => string[]>(
    'ADDITIONAL_I18N_PROVIDER',
);
