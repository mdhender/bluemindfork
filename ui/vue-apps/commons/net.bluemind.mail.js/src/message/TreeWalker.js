/**
 * Walk a tree of nodes having a 'children' property.
 * Call the 'visit(node, ancestors)' method of each given visitor.
 */
export default class TreeWalker {
    constructor(rootNode, visitors) {
        this.rootNode = rootNode;
        this.visitors = visitors;
    }

    walk() {
        this._walk(this.rootNode, []);
    }

    _walk(node, ancestors) {
        this.visitors.forEach(visitor => visitor.visit(node, ancestors));
        // Ancestors reference must not be modified by children.
        // Indeed, we must not share the same ancestors in the different branches
        const localAncestors = ancestors.slice(0);
        localAncestors.push(node);
        if (node.children) {
            node.children.forEach(child => {
                this._walk(child, localAncestors);
            });
        }
    }
}
