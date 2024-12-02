import { Injectable, TemplateRef } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { OptionItem } from 'ngx-edu-sharing-ui';
import { Node } from 'ngx-edu-sharing-api';

export type PostProcessOptionsCallback = (options: OptionItem[], objects: Node[]) => void;
/**
 * Service to configure global Search Service behaviour, i.e. custom post processing of options
 */
@Injectable({
    providedIn: 'root',
})
export class GlobalOptionsService {
    /**
     * register a callback that is fired when all global options are generated
     */
    postPrepareOptions: PostProcessOptionsCallback;
}
