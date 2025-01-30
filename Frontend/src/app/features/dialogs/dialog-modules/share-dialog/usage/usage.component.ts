import { Component, EventEmitter, Input, Output } from '@angular/core';
import { UIAnimation } from 'ngx-edu-sharing-ui';
import { trigger } from '@angular/animations';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'es-share-dialog-usage',
    templateUrl: 'usage.component.html',
    styleUrls: ['usage.component.scss'],
    animations: [trigger('cardAnimation', UIAnimation.cardAnimation())],
})
export class ShareDialogUsageComponent {
    static ICON_MAP: any = {
        MOODLE: 'school',
        COLLECTION: 'layers',
    };
    @Input() name: string;
    @Input() usages: any[];
    @Input() deleteList: any[];
    @Output() deleteListChange = new EventEmitter();
    @Input() showDelete = true;
    showAll = false;
    isDeleted(usage: any) {
        return this.deleteList.indexOf(usage) != -1;
    }
    getIcon() {
        let map = ShareDialogUsageComponent.ICON_MAP[this.name.toUpperCase()];
        if (map) return map;
        return 'extension';
    }
    getName(usage: any) {
        // may be a collection
        if (usage.collection) return usage.collection.title;

        let info = usage.usageXmlParams;
        if (info && info.general.referencedInName != null) {
            const typeStr =
                'WORKSPACE.SHARE.USAGE_SUBTYPE.' + info.general.referencedInType.toUpperCase();
            let type = this.translation.instant(typeStr);
            if (type === typeStr) {
                type = info.general.referencedInType;
            }
            return this.translation.instant('WORKSPACE.SHARE.USAGE_INFO', {
                instance: info.general.referencedInInstance,
                type,
                name: info.general.referencedInName,
            });
        }
        return usage.courseId;
    }
    public remove(usage: any) {
        if (this.showDelete && usage.type != 'INDIRECT') {
            if (this.isDeleted(usage)) this.deleteList.splice(this.deleteList.indexOf(usage), 1);
            else this.deleteList.push(usage);
            this.deleteListChange.emit(this.deleteList);
        }
    }
    constructor(private translation: TranslateService) {}
}
