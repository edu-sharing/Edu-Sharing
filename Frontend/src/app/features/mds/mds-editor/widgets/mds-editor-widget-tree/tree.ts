import { MdsWidget, MdsWidgetValue } from '../../../types/types';
import { DisplayValue } from '../DisplayValues';

let nextUniqueId = 0;

export class TreeNode {
    id: string;
    alternativeIds: string[];
    uid: string;
    caption: string;
    children?: TreeNode[];
    parent?: TreeNode;
    isChecked?: boolean;
    isIndeterminate?: boolean;
    isHidden?: boolean;
    type?: null | 'suggestionInput';
}

export type MdsWidgetTree = MdsWidget & {
    tree: Tree;
};

export class Tree {
    rootNodes: TreeNode[];
    nodesMap: { [key: string]: TreeNode };
    nodesMapAlternativeIds: { [key: string]: TreeNode };

    static generateTree(
        definedValues: readonly MdsWidgetValue[],
        checkedValues: readonly string[] = [],
        indeterminateValues?: readonly string[],
    ): Tree {
        const tree = new Tree();
        let remainingValues = definedValues;
        while (remainingValues.length > 0) {
            const newRemainingValues = [];
            for (const value of remainingValues) {
                if (value.id in tree.nodesMap) {
                    console.error('Encountered duplicate id when generating tree:', value.id);
                    continue;
                }
                const node: TreeNode = {
                    id: value.id,
                    alternativeIds: value.alternativeIds,
                    uid: `app-tree-node-${nextUniqueId++}`,
                    caption: value.caption,
                    isChecked: checkedValues.includes(value.id),
                    isIndeterminate: indeterminateValues?.includes(value.id),
                    isHidden: false,
                };
                if (!value.parent) {
                    tree.pushNode(node);
                } else {
                    const parent = tree.nodesMap[value.parent];
                    if (parent) {
                        tree.pushNode(node, parent);
                    } else {
                        newRemainingValues.push(value);
                    }
                }
            }
            if (remainingValues.length === newRemainingValues.length) {
                console.error('Failed to find parents for the following nodes:', remainingValues);
                break;
            } else {
                remainingValues = newRemainingValues;
            }
        }
        return tree;
    }

    private constructor() {
        this.rootNodes = [];
        this.nodesMap = {};
        this.nodesMapAlternativeIds = {};
    }

    findById(id: string): TreeNode {
        return this.nodesMap[id];
    }

    find(
        predicate: (node: TreeNode) => boolean,
        rootNodes: TreeNode[] = this.rootNodes,
    ): TreeNode[] {
        let foundNodes: TreeNode[] = [];
        for (const node of rootNodes) {
            if (predicate(node)) {
                foundNodes.push(node);
            }
            if (node.children) {
                foundNodes = [...foundNodes, ...this.find(predicate, node.children)];
            }
        }
        return foundNodes;
    }

    *iterate(rootNodes: TreeNode[] = this.rootNodes): Generator<TreeNode> {
        for (const node of rootNodes) {
            yield node;
            if (node.children) {
                for (const childNode of this.iterate(node.children)) {
                    yield childNode;
                }
            }
        }
    }

    getAncestors(node: TreeNode): TreeNode[] {
        const ancestors: TreeNode[] = [];
        while (node) {
            ancestors.push(node);
            node = node.parent;
        }
        return ancestors;
    }

    toDisplayValue(id: string): DisplayValue {
        const node = this.nodesMap[id] || this.nodesMapAlternativeIds[id];
        if (node == null) {
            return {
                key: id,
                label: id,
            };
        }
        return this.nodeToDisplayValue(node);
    }

    nodeToDisplayValue(node: TreeNode): DisplayValue {
        return {
            key: node.id,
            label: node.caption,
            hint: this.getFullDisplayValue(node),
        };
    }

    private pushNode(node: TreeNode, parent?: TreeNode): void {
        this.nodesMap[node.id] = node;
        if (node.alternativeIds?.length > 0) {
            // FIXME: we're currently only supporting one alternative id but not multiple
            this.nodesMapAlternativeIds[node.alternativeIds[0]] = node;
        }
        if (parent) {
            node.parent = parent;
            if (parent.children) {
                parent.children.push(node);
            } else {
                parent.children = [node];
            }
        } else {
            this.rootNodes.push(node);
        }
    }

    private getFullDisplayValue(node: TreeNode): string {
        let result = node.caption;
        let parent = node.parent;
        while (parent) {
            result = `${parent.caption} / ${result}`;
            parent = parent.parent;
        }
        return result;
    }
}
