import { RouterComponent } from './router/router.component';
import { NodeRenderComponent } from './common/ui/node-render/node-render.component';
import { ApplyToLmsComponent } from './common/ui/apply-to-lms/apply-to-lms.component';
import { MdsTestComponent } from './common/test/mds-test/mds-test.component';
import { ToolListComponent } from './common/ui/tool-list/tool-list.component';
import { ToucheventDirective } from './common/ui/touchevents/touchevents';
import { InfobarComponent } from './common/ui/infobar/infobar.component';
import { AutocompleteComponent } from './common/ui/autocomplete/autocomplete.component';
import { CookieInfoComponent } from './common/ui/cookie-info/cookie-info.component';
import { PoweredByComponent } from './common/ui/powered-by/powered-by.component';
import { GlobalContainerComponent } from './common/ui/global-container/global-container.component';
import { RocketchatComponent } from './common/ui/global-container/rocketchat/rocketchat.component';
import { ButtonsTestComponent } from './common/test/buttons/buttons-test.component';
import { InputsTestComponent } from './common/test/inputs/inputs-test.component';
import { UserAvatarTestComponent } from './common/test/user-avatar/user-avatar-test.component';
import { ModalTestComponent } from './common/test/modal/modal-test.component';
import { ToolpermissionCheckDirective } from './common/directives/toolpermission-check.directive';
import { ColorTransformPipe } from './common/ui/color-transform.pipe';

import { MatConfirmGroupComponent } from './common/ui/mat-confirm-group/mat-confirm-group.component';
import { ScrollToTopButtonComponent } from './common/ui/scroll-to-top-button/scroll-to-top-button.component';
import { CustomGlobalExtensionsComponent } from './extension/custom-global-component/custom-global-extensions.component';

export const DECLARATIONS = [
    MdsTestComponent,
    ButtonsTestComponent,
    InputsTestComponent,
    UserAvatarTestComponent,
    ModalTestComponent,
    AutocompleteComponent,
    ColorTransformPipe,
    ScrollToTopButtonComponent,
    CustomGlobalExtensionsComponent,
    GlobalContainerComponent,
    RocketchatComponent,
    RouterComponent,
    InfobarComponent,
    NodeRenderComponent,
    ApplyToLmsComponent,
    ToolListComponent,
    ToucheventDirective,
    PoweredByComponent,
    CookieInfoComponent,
    MatConfirmGroupComponent,
    ToolpermissionCheckDirective,
];
