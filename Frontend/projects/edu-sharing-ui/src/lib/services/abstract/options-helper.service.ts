import { OptionData, OptionsHelperComponents } from '../options-helper-data.service';
import { OptionItem, Target } from '../../types/option-item';
import { Node } from 'ngx-edu-sharing-api';

export abstract class OptionsHelperService {
    abstract wrapOptionCallbacks(data: OptionData): OptionData;

    abstract refreshComponents(
        components: OptionsHelperComponents,
        data: OptionData,
    ): Promise<void>;

    abstract getAvailableOptions(
        target: Target,
        objects: Node[],
        components: OptionsHelperComponents,
        data: OptionData,
    ): Promise<OptionItem[]>;

    abstract pasteNode(
        components: OptionsHelperComponents,
        data: OptionData,
        addVirutalNodes: boolean,
        nodes: Node[],
    ): void;

    abstract filterOptions(
        options: OptionItem[],
        target: Target,
        data: OptionData,
        objects: Node[] | any,
    ): Promise<OptionItem[]>;
}
