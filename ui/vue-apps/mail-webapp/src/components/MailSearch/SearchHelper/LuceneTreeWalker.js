export default class LuceneTreeWalker {
    static walk(node, nodeFunction, payload) {
        payload = nodeFunction(node, payload);
        if (node.left) {
            this.walk(node.left, nodeFunction, payload);

            if (node.right) {
                this.walk(node.right, nodeFunction, payload);
            }
        }
    }
}
