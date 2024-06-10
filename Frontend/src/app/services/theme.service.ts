import { Inject, Injectable } from '@angular/core';
import { MaterialCssVarsService } from 'angular-material-css-vars';
import { HueValue } from 'angular-material-css-vars/lib/model';
import { ConfigService, ConfigThemeColor } from 'ngx-edu-sharing-api';
import { EDU_SHARING_UI_CONFIG, EduSharingUiConfiguration } from 'ngx-edu-sharing-ui';

export enum Variable {
    Primary = 'primary',
    Accent = 'accent',
    Warn = 'warn',
}
@Injectable({ providedIn: 'root' })
export class ThemeService {
    constructor(
        private materialCssVarsService: MaterialCssVarsService,
        private configService: ConfigService,
        @Inject(EDU_SHARING_UI_CONFIG) private uiConfig: EduSharingUiConfiguration,
    ) {
        // set defaults
        this.initWithDefaults();
    }

    initWithDefaults() {
        this.setColor(Variable.Primary, '#48708e');
        this.setColor(Variable.Accent, '#48708e');
        this.setColor(Variable.Warn, '#cd2457');
        this.setViaConfig();
    }

    private setViaConfig() {
        this.configService.observeConfig().subscribe(
            (config) => {
                const colors = config.themeColors?.color;
                if (colors) {
                    console.info('apply branding from config', colors);
                    this.applyFromConfigColors(colors);
                } else {
                    console.info('no branding colors in config, using defaults');
                }
            },
            (error) => {
                console.warn(
                    'Theme service failed to observe config, no branding colors applied',
                    error,
                );
            },
        );
    }

    applyFromConfigColors(colors: Array<ConfigThemeColor>) {
        colors.forEach((c) => this.setColor(c.variable, c.value));
    }

    setColor(variable: Variable | string, color: string) {
        document.documentElement.style.setProperty('--' + variable, color);
        switch (variable) {
            case Variable.Primary:
                this.materialCssVarsService.setPrimaryColor(color);
                /*this.materialCssVarsService.setVariable(
                    MaterialCssVariables.ForegroundDivider,
                    this.fromPalette(color, '500'),
                );*/
                if (!this.uiConfig.isEmbedded) {
                    document
                        .querySelector('meta[name="theme-color"]')
                        .setAttribute('content', color);
                }
                break;
            case Variable.Accent:
                this.materialCssVarsService.setAccentColor(color);
                break;
            case Variable.Warn:
                this.materialCssVarsService.setWarnColor(color);
                break;
        }
    }

    private fromPalette(color: string, hueValue: HueValue) {
        const palette = this.materialCssVarsService.getPaletteWithContrastForColor(color);
        const result = palette.filter((p) => p.hue === hueValue)[0].color;
        return `${result.r}, ${result.g}, ${result.b}`;
    }
}
