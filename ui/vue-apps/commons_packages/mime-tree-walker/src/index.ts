import { MessageBody } from "@bluemind/backend.mail.api";
import Visitor from "./Visitor";
import GetLeafsVisitor from "./GetLeafsVisitor";

/**
 * Walk a tree of nodes having a 'children' property.
 * Call the 'visit(node, ancestors)' method of each given visitor.
 */
export default class TreeWalker {
    rootNode: MessageBody.Part;
    visitors: Visitor[];
    constructor(rootNode: MessageBody.Part, visitors: Visitor[]) {
        this.rootNode = rootNode;
        this.visitors = visitors;
    }

    walk() {
        this._walk(this.rootNode, []);
    }

    _walk(node: MessageBody.Part, ancestors: MessageBody.Part[] = []) {
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

export { Visitor, GetLeafsVisitor };
