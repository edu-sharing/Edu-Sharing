import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { AddWithConnectorDialogComponent } from './add-with-connector-dialog.component';
import { MdsModule } from '../../../mds/mds.module';

export { AddWithConnectorDialogComponent };

@NgModule({
    declarations: [AddWithConnectorDialogComponent],
    imports: [SharedModule, MdsModule],
})
export class AddWithConnectorDialogModule {}
