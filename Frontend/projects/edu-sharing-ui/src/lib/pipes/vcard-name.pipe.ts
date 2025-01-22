import { Pipe, PipeTransform } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { VCard } from '../util/VCard';

@Pipe({ name: 'vcardName' })
export class VCardNamePipe implements PipeTransform {
    constructor(private translate: TranslateService) {}
    transform(authority: string | string[], args: string[] = null): string {
        if (Array.isArray(authority)) {
            return authority
                .map((a) => (a ? new VCard(a).getDisplayName() : ''))
                .filter((s) => !!s)
                .join(', ');
        }
        return authority ? new VCard(authority).getDisplayName() : '';
    }
}
