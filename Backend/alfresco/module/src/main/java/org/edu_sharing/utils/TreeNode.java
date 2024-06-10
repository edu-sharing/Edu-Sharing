package org.edu_sharing.utils;

import lombok.Getter;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TreeNode<V> {
    @Getter
    private final V item;

    private final List<TreeNode<V>> children;

    public TreeNode() {
        this.item = null;
        this.children = new ArrayList<>();
    }



    public TreeNode(ArrayList<TreeNode<V>> children) {
        this.item = null;
        this.children = new ArrayList<>(children);
    }

    public TreeNode(V element) {
        this.item = element;
        this.children = new ArrayList<>();
    }

    public List<TreeNode<V>> getChildren() {
        return children;
    }

    public List<V> getChildElements() {
        return children.stream().map(TreeNode::getItem).collect(Collectors.toList());
    }

    public void put(V element) {
        children.add(new TreeNode<>(element));
    }

    public void put(TreeNode<V> node) {
        children.add(node);
    }

    public TreeNode<V>  remove(V element) {
        int index = IntStream.of(children.size()).filter(i -> Objects.equals(children.get(i).getItem(), element)).findFirst().orElse(-1);
        if(index == -1){
            return null;
        }

        return children.remove(index);
    }

    public boolean remove(TreeNode<V> element) {
        return children.remove(element);
    }


    /**
     * Performs a depth first search and returns the first element found by the given key
     * @param element - element to search for
     * @return The first TreeNode with the given key or null
     */
    public TreeNode<V> find(V element) {
        if(getItem().equals(element)){
            return this;
        }

        TreeNode<V> result;
        for (TreeNode<V> node : children){
            result = node.find(element);
            if(result != null) {
                return result;
            }
        }
        return null;
    }

    /**
     * Performs a depth first search and returns the first element found by the given key
     * @param predicate - element to search for
     * @return The first TreeNode with the given key or null
     */
    public TreeNode<V> find(Function<V, Boolean> predicate) {
        if(predicate.apply(item)){
            return this;
        }

        TreeNode<V> result;
        for (TreeNode<V> node : children){
            result = node.find(predicate);
            if(result != null) {
                return result;
            }
        }
        return null;
    }


    /**
     * Checks if there is no other node under this branch
     * @return true if there is no other node under this branch
     */
    public boolean isLeaf() {
        return children.isEmpty();
    }

    /**
     * Performs the given action for each element of the Iterable until all elements have been processed or the action throws an exception. Actions are performed in the depth first order of tree structure. Exceptions thrown by the action are relayed to the caller.
     * The behavior of this method is unspecified if the action performs side-effects that modify the underlying source of elements, unless an overriding class has specified a concurrent modification policy.
     * @param action The action to be performed for each element
     * @throws NullPointerException  if the specified action is null
     */
    public void traverse(Consumer<V> action) throws NullPointerException {
        action.accept(item);
        children.forEach(x->x.traverse(action));
    }

    public static <K, V> TreeNode< V> of(TreeNode<V>... nodes) {
        return of(null, nodes);
    }

    public static <V> TreeNode<V> of(V rootElement, TreeNode<V>... nodes) {
        TreeNode<V> root = new TreeNode<>(rootElement);
        for (TreeNode<V> node : nodes){
            root.put(node);
        }
        return root;
    }

    public static <V, K> TreeNode<V> of(Collection<V> collection, Function<V, K> keyProvider, Function<V, K> parentKeyProvider) {
        return of(null, collection, keyProvider, parentKeyProvider);
    }

    public static <K, V> TreeNode<V> of(V rootElement, Collection<V> collection, Function<V, K> keyProvider, Function<V, K> parentKeyProvider) {
        TreeNode<V> root = new TreeNode<>(rootElement);
        Map<K, TreeNode<V>> nodes = new LinkedHashMap<>();
        collection.forEach(x -> {
            K parentKey = parentKeyProvider.apply(x);
            TreeNode<V> node = new TreeNode<>(x);
            nodes.put(keyProvider.apply(node.getItem()), node);
            TreeNode<V> parentNode = parentKey == null ? root : nodes.getOrDefault(parentKey, root);
            parentNode.put(node);
        });

        // remove root if there is only one element
        if (root.getChildren().size() == 1) {
            return root.getChildren().get(0);
        }
        return root;
    }
}
