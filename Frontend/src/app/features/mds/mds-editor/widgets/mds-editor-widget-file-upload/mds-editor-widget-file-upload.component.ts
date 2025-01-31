import { ChangeDetectorRef, Component, EventEmitter, Output } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { NativeWidgetComponent } from '../../mds-editor-view/mds-editor-view.component';
import { InputStatus, Values } from '../../../types/types';

@Component({
    selector: 'es-mds-editor-widget-file-upload',
    templateUrl: './mds-editor-widget-file-upload.component.html',
    styleUrls: ['./mds-editor-widget-file-upload.component.scss'],
})
export class MdsEditorWidgetFileUploadComponent implements NativeWidgetComponent {
    static readonly constraints = {
        requiresNode: false,
        supportsBulk: false,
    };
    selectedFiles = new BehaviorSubject<File[]>(null);
    hasChanges = new BehaviorSubject<boolean>(false);
    isFileOver = false;
    supportsDrop = true;
    _link: string;
    get link() {
        return this._link;
    }
    set link(link: string) {
        this._link = link;
        this.update();
    }
    status = new BehaviorSubject<InputStatus>('INVALID');

    @Output() setLink = new EventEmitter<string>();

    constructor(private changeDetectorRef: ChangeDetectorRef) {}

    doSetLink() {
        this.setLink.emit(this.link);
    }

    setFilesByFileList(fileList: FileList) {
        if (this.link) {
            return;
        }
        const selectedFiles = [];
        for (let i = 0; i < fileList.length; i++) {
            selectedFiles.push(fileList.item(i));
        }
        this.selectedFiles.next(selectedFiles);
        this.update();
    }

    filesSelected(files: Event) {
        this.setFilesByFileList((files.target as HTMLInputElement).files);
    }

    async getValues(values: Values) {
        if (this.selectedFiles.value?.length) {
            const file = this.selectedFiles.value[0];
            const base64 = await new Promise((resolve, reject) => {
                const reader = new FileReader();
                reader.readAsDataURL(file);
                reader.onload = () => resolve(reader.result);
            });
            values['fileupload-filename'] = [file.name];
            values['fileupload-filetype'] = [file.type];
            values['fileupload-filedata'] = [base64 as string];
        } else {
            values['fileupload-link'] = [this.link];
        }
        return values;
    }
    clearFile() {
        this.selectedFiles.next(null);
        this.changeDetectorRef.detectChanges();
    }
    private update() {
        this.hasChanges.next(!!this.selectedFiles.value?.length || !!this._link);
        this.status.next(this.hasChanges.value ? 'VALID' : 'INVALID');
        this.changeDetectorRef.detectChanges();
    }
}
