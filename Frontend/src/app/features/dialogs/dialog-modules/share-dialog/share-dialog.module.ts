import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { ShareDialogChooseTypeComponent } from './choose-type/choose-type.component';
import { FocusableOptionDirective } from './choose-type/focusable-option.directive';
import { ShareDialogPermissionComponent } from './permission/permission.component';
import { NodeAuthorNamePipe } from './publish/node-author-name.pipe';
import { ShareDialogPublishComponent } from './publish/publish.component';
import { ShareDialogComponent } from './share-dialog.component';
import { ShareDialogUsageComponent } from './usage/usage.component';
import { ShareDialogRestrictedAccessComponent } from './restricted-access/restricted-access.component';
import { ShareDialogChooseDateComponent } from './permission/choose-date/choose-date.component';

export { ShareDialogComponent };

@NgModule({
    declarations: [
        FocusableOptionDirective,
        NodeAuthorNamePipe,
        ShareDialogChooseTypeComponent,
        ShareDialogComponent,
        ShareDialogPermissionComponent,
        ShareDialogChooseDateComponent,
        ShareDialogPublishComponent,
        ShareDialogUsageComponent,
        ShareDialogRestrictedAccessComponent,
    ],
    imports: [SharedModule],
})
export class ShareDialogModule {}
