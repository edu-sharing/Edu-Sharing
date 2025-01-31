import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { provideAnimations } from '@angular/platform-browser/animations';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import { ApiRequestConfiguration, EduSharingApiModule } from 'ngx-edu-sharing-api';
import { translateProvider } from '../../../../../src/app/features/mds/mds-editor/storybook-utils';
import { EduSharingUiCommonModule } from '../common/edu-sharing-ui-common.module';
import { OptionItem } from '../types/option-item';
import { ActionbarComponent } from './actionbar.component';

let defaultOptions = [
    new OptionItem('Option 1', 'home', () => {}),
    new OptionItem('Option 2', 'help_outline', () => {}),
    new OptionItem('Option 3', 'help_outline', () => {}),
    new OptionItem('Option 4', 'help_outline', () => {}),
    new OptionItem('Option 5', 'help_outline', () => {}),
    new OptionItem('Option 6', 'help_outline', () => {}),
    new OptionItem('Option 7', 'help_outline', () => {}),
];
const actionbar: Meta<ActionbarComponent> = {
    title: 'Core UI/Actionbar',
    component: ActionbarComponent,
    decorators: [
        moduleMetadata({
            imports: [
                MatMenuModule,
                MatButtonModule,
                EduSharingApiModule,
                EduSharingUiCommonModule,
            ],
            declarations: [],
        }),
        applicationConfig({
            providers: [
                provideAnimations(),
                {
                    provide: TranslatePipe,
                    useValue: {},
                },
                {
                    provide: ApiRequestConfiguration,
                    useValue: {},
                },
                {
                    provide: TranslateService,
                    useValue: translateProvider,
                },
            ],
        }),
    ],
    args: {
        backgroundType: 'primary',
        appearance: 'button',
        options: defaultOptions,
        numberOfAlwaysVisibleOptions: 2,
        numberOfAlwaysVisibleOptionsMobile: 1,
    },
    argTypes: {
        appearance: {
            control: 'select',
            options: ['button', 'round', 'icon-button'],
        },
    },
    tags: ['autodocs'],
};

export default actionbar;
type Story = StoryObj<ActionbarComponent>;
export const ActionbarPrimary: Story = {
    args: {},
};
export const ActionbarPrimaryOneOption: Story = {
    args: {
        options: [new OptionItem('Option 1', 'home', () => {})],
    },
};
export const ActionbarPrimaryTwoOptions: Story = {
    args: {
        options: [
            new OptionItem('Option 1', 'home', () => {}),
            new OptionItem('Option 2', 'help_outline', () => {}),
        ],
    },
};
export const ActionbarPrimaryOptionsDisabled: Story = {
    args: {
        options: defaultOptions.map((o) => {
            o = new OptionItem(o.name, o.icon, o.callback);
            o.isEnabled = false;
            return o;
        }),
    },
};
export const ActionbarPrimaryOptionsDisabledMenu: Story = {
    args: {
        options: defaultOptions.map((o, i) => {
            o = new OptionItem(o.name, o.icon, o.callback);
            o.isEnabled = i > 3;
            return o;
        }),
    },
};
export const ActionbarRound: Story = {
    args: {
        appearance: 'round',
    },
};
export const ActionbarOnDarkBackground: Story = {
    args: {
        backgroundType: 'dark',
    },
};
export const ActionbarOnBrightBackground: Story = {
    args: {
        backgroundType: 'bright',
    },
};
