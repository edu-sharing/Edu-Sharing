import { NestedTreeControl } from '@angular/cdk/tree';
import {
    ChangeDetectorRef,
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    SimpleChanges,
    ViewChild,
} from '@angular/core';
import { MatTreeNestedDataSource } from '@angular/material/tree';
import { BehaviorSubject, ReplaySubject } from 'rxjs';
import { distinctUntilChanged, map, takeUntil } from 'rxjs/operators';
import { MdsEditorInstanceService, Widget } from '../../../mds-editor-instance.service';
import { MdsWidgetType } from '../../../../types/types';
import { DisplayValue } from '../../DisplayValues';
import { Tree, TreeNode } from '../tree';
import { Toast } from '../../../../../../services/toast';
import { Helper } from '../../../../../../core-module/rest/helper';
import { RestConstants } from '../../../../../../core-module/rest/rest-constants';
import { MdsV1Service } from 'ngx-edu-sharing-api';

let nextUniqueId = 0;

@Component({
    selector: 'es-mds-editor-widget-tree-core',
    templateUrl: './mds-editor-widget-tree-core.component.html',
    styleUrls: ['./mds-editor-widget-tree-core.component.scss'],
})
export class MdsEditorWidgetTreeCoreComponent implements OnInit, OnChanges, OnDestroy {
    readonly uid = `app-mds-editor-widget-tree-core-${nextUniqueId++}`;
    @ViewChild('input') input: ElementRef;
    @Input() widget: Widget;
    @Input() tree: Tree;
    @Input() values: DisplayValue[];
    @Input() indeterminateValues: string[];
    suggesting: boolean;
    get filterString() {
        return this.filterString$.value;
    }
    set filterString(filterString: string) {
        this.filterString$.next(filterString);
    }
    /**
     * Whether a checked parent node should visually indicate that child nodes are checked as well.
     *
     * Checkboxes of child nodes will be disabled in this case.
     *
     * Not compatible with single-value mode.
     */
    @Input() parentImpliesChildren = false;

    @Output() closeTree = new EventEmitter<void>();
    @Output() valuesChange = new EventEmitter<DisplayValue[]>();
    @Output() indeterminateValuesChange = new EventEmitter<string[]>();

    treeControl = new NestedTreeControl<TreeNode>((node) => node.children);
    dataSource = new MatTreeNestedDataSource<TreeNode>();
    selectedNode: TreeNode;
    isMultiValue: boolean;

    private filterString$ = new BehaviorSubject<string>(null);
    private destroyed$: ReplaySubject<void> = new ReplaySubject(1);
    constructor(
        private toast: Toast,
        public changeDetectorRef: ChangeDetectorRef,
        private mdsEditorInstanceService: MdsEditorInstanceService,
        private mdsService: MdsV1Service,
    ) {}
    ngOnInit(): void {
        this.isMultiValue = this.widget.definition.type === MdsWidgetType.MultiValueTree;
        this.clearFilter();
        // deep copy for modifications
        this.dataSource.data = this.tree.rootNodes;
        if (
            this.widget.definition.allowValuespaceSuggestions &&
            this.mdsEditorInstanceService.editorMode === 'nodes'
        ) {
            this.dataSource.data = this.addSuggestionInput(
                Helper.deepCopyArray(this.dataSource.data),
            );
        }
        this.filterString$
            .pipe(
                map((filterString) => (filterString?.length >= 2 ? filterString : null)),
                distinctUntilChanged(),
                takeUntil(this.destroyed$),
            )
            .subscribe((filterString) => {
                this.filterNodes(filterString);
            });
        this.treeControl.expansionModel.changed.subscribe((change) => {
            for (const expandedNode of change.added) {
                if (expandedNode.children?.every((child) => child.isHidden)) {
                    expandedNode.children.forEach((child) => (child.isHidden = false));
                }
            }
        });
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.filterString) {
            this.filterString$.next(changes.filterString.currentValue);
        }
    }

    ngOnDestroy() {
        this.destroyed$.next();
        this.destroyed$.complete();
    }

    revealInTree(node: TreeNode): void {
        let parent = node.parent;
        while (parent) {
            this.treeControl.expand(parent);
            parent = parent.parent;
        }
        setTimeout(() => this.selectNode(node));
    }

    handleKeydown(keyCode: string): boolean {
        if (keyCode === 'ArrowDown') {
            this.selectNode(this.findNextVisibleNode(this.selectedNode));
            return true;
        } else if (keyCode === 'ArrowUp') {
            this.selectNode(this.findPreviousVisibleNode(this.selectedNode));
            return true;
        }
        if (this.selectedNode) {
            if (keyCode === 'ArrowLeft') {
                if (this.treeControl.isExpanded(this.selectedNode)) {
                    this.treeControl.collapse(this.selectedNode);
                } else if (this.selectedNode.parent) {
                    this.selectNode(this.selectedNode.parent);
                }
                return true;
            } else if (keyCode === 'ArrowRight') {
                if (
                    this.selectedNode.children?.length > 0 &&
                    !this.treeControl.isExpanded(this.selectedNode)
                ) {
                    this.treeControl.expand(this.selectedNode);
                }
                return true;
            } else if (keyCode === 'Space') {
                if (!this.getIsDisabled(this.selectedNode)) {
                    this.toggleNode(this.selectedNode);
                }
                return true;
            }
        }
        return false;
    }

    hasChild(_: number, node: TreeNode) {
        return !!node.children && node.children.length > 0;
    }

    toggleNode(node: TreeNode, checked?: boolean, byUser = true): void {
        checked = checked ?? !node.isChecked;
        if (checked && !this.isMultiValue) {
            this.clearAll();
        }
        node.isChecked = checked;
        if (node.isChecked) {
            this.add(node);
        } else {
            this.remove(node);
        }
        this.valuesChange.emit(this.values);
        this.removeFromIndeterminateValues(node);
        if (node.isChecked && node.children && this.parentImpliesChildren) {
            // Toggle any checked child nodes off since they are already implicitly checked by this
            // node.
            for (const childNode of this.tree.iterate(node.children)) {
                if (childNode.isChecked) {
                    this.toggleNode(childNode, false, false);
                }
            }
        }
        if (byUser) {
            document
                .getElementById(this.getCheckboxId(node) + '-state')
                .setAttribute('role', 'alert');
        }
        this.changeDetectorRef.detectChanges();
    }

    getCheckboxId(node: TreeNode): string {
        return `${node.uid}-checkbox`;
    }

    hasCheckedAncestor(node: TreeNode): boolean {
        while (node) {
            node = node.parent;
            if (node?.isChecked) {
                return true;
            }
        }
        return false;
    }

    getIsDisabled(node: TreeNode): boolean {
        if (this.parentImpliesChildren) {
            return this.hasCheckedAncestor(node);
        } else {
            return false;
        }
    }

    getIsChecked(node: TreeNode): boolean {
        if (this.parentImpliesChildren) {
            return node.isChecked || this.hasCheckedAncestor(node);
        } else {
            return node.isChecked;
        }
    }

    /** Call only via `toggleNode`. */
    private add(node: TreeNode): void {
        if (!this.values.find((value) => node.id === value.key)) {
            this.values.push(this.tree.nodeToDisplayValue(node));
        }
    }

    /** Call only via `toggleNode`. */
    private remove(node: TreeNode): void {
        const index = this.values.findIndex((value) => node.id === value.key);
        if (index >= 0) {
            this.values.splice(index, 1);
        }
    }

    private removeFromIndeterminateValues(node: TreeNode): void {
        node.isIndeterminate = false;
        if (this.indeterminateValues?.includes(node.id)) {
            this.indeterminateValues.splice(this.indeterminateValues.indexOf(node.id), 1);
            this.indeterminateValuesChange.emit(this.indeterminateValues);
        }
    }

    private clearAll(): void {
        for (const value of this.values) {
            this.tree.findById(value.key).isChecked = false;
        }
        this.values = [];
        if (this.indeterminateValues) {
            for (const key of this.indeterminateValues) {
                this.tree.findById(key).isIndeterminate = false;
            }
            this.indeterminateValues = null;
            this.indeterminateValuesChange.emit(null);
        }
    }

    private clearFilter(): void {
        for (const node of this.tree.iterate()) {
            node.isHidden = false;
        }
    }

    private filterNodes(filterString: string): void {
        const MAX_EXPAND_NODES = 30;
        let expandedNodes = 0;
        if (!filterString) {
            this.clearFilter();
            return;
        }
        this.treeControl.collapseAll();
        for (const node of this.tree.iterate()) {
            node.isHidden = true;
        }
        const filteredNodes = this.getFilteredNodes(filterString);
        for (const node of filteredNodes) {
            for (const ancestor of this.tree.getAncestors(node)) {
                ancestor.isHidden = false;
                if (ancestor !== node && expandedNodes++ <= MAX_EXPAND_NODES) {
                    this.treeControl.expand(ancestor);
                }
            }
        }
    }

    private getFilteredNodes(filterString: string): TreeNode[] {
        return this.tree.find((node) => {
            if (!node.caption) {
                return false;
            }
            const nodeWords = node.caption.trim().toLowerCase().split(/\s+/);
            const filterWords = filterString.trim().toLowerCase().split(/\s+/);
            return filterWords.every((filterWord) =>
                nodeWords.some((nodeWord) => nodeWord.indexOf(filterWord) !== -1),
            );
        });
    }

    /** Selects node for keyboard navigation (not checkbox). */
    private selectNode(node: TreeNode): void {
        this.selectedNode = node;
        if (this.selectedNode) {
            this.scrollIntoView(this.selectedNode);
        }
    }

    private scrollIntoView(node: TreeNode, options: ScrollIntoViewOptions = {}): void {
        document
            .getElementById(this.getCheckboxId(node))
            .scrollIntoView({ behavior: 'smooth', block: 'center', ...options });
    }

    private findNextVisibleNode(node?: TreeNode): TreeNode | null {
        do {
            node = this.findNextExpandedNode(node);
        } while (!!node && node.isHidden);
        return node;
    }

    private findPreviousVisibleNode(node?: TreeNode): TreeNode | null {
        do {
            node = this.findPreviousExpandedNode(node);
        } while (!!node && node.isHidden);
        return node;
    }

    private findNextExpandedNode(node?: TreeNode): TreeNode | null {
        if (!node) {
            return this.tree.rootNodes[0];
        } else if (node.children?.length > 0 && this.treeControl.isExpanded(node)) {
            return node.children[0];
        }
        while (node) {
            const sibling = this.findNextSibling(node);
            if (sibling) {
                return sibling;
            } else {
                node = node.parent;
            }
        }
        return null;
    }

    private findPreviousExpandedNode(node?: TreeNode): TreeNode | null {
        if (!node) {
            return this.findLastExpandedDescendent(
                this.tree.rootNodes[this.tree.rootNodes.length - 1],
            );
        }
        const previousSibling = this.findPreviousSibling(node);
        if (previousSibling) {
            return this.findLastExpandedDescendent(previousSibling);
        }
        return node.parent;
    }

    private findLastExpandedDescendent(node: TreeNode): TreeNode {
        while (this.treeControl.isExpanded(node) && node.children) {
            node = node.children[node.children.length - 1];
        }
        return node;
    }

    private findNextSibling(node: TreeNode): TreeNode | null {
        const nodesList = node.parent?.children ?? this.tree.rootNodes;
        if (nodesList && nodesList.length >= nodesList.indexOf(node)) {
            return nodesList[nodesList.indexOf(node) + 1];
        } else {
            return null;
        }
    }

    private findPreviousSibling(node: TreeNode): TreeNode | null {
        const nodesList = node.parent?.children ?? this.tree.rootNodes;
        if (nodesList && nodesList.indexOf(node) > 0) {
            return nodesList[nodesList.indexOf(node) - 1];
        } else {
            return null;
        }
    }

    private addSuggestionInput(data: TreeNode[], parent: TreeNode = null) {
        data.filter((t) => t.children).forEach(
            (t) => (t.children = this.addSuggestionInput(t.children, t)),
        );
        // already processed, skip
        if (data.some((d) => d.type === 'suggestionInput')) {
            return data;
        }
        return data.concat([
            {
                id: null,
                alternativeIds: null,
                uid: null,
                caption: null,
                parent,
                type: 'suggestionInput',
            },
        ]);
    }

    async suggestValue(value: string, node: TreeNode) {
        try {
            this.suggesting = true;
            await this.mdsService
                .suggestValue({
                    repository: RestConstants.HOME_REPOSITORY,
                    widget: this.widget.definition.id,
                    metadataset: this.mdsEditorInstanceService.mdsId,
                    parent: node.parent?.id,
                    nodeId: this.mdsEditorInstanceService.nodes$.value?.map((n) => n.ref.id),
                    caption: value,
                })
                .toPromise();
            this.toast.toast('MDS.SUGGEST_VALUE_SENT');
        } catch (e) {
            // Do nothing
        }
        this.suggesting = false;
    }
}
