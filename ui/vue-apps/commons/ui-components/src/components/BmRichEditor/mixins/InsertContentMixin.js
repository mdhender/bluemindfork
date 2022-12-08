export default {
    data() {
        return { nonEditableContent: null };
    },
    methods: {
        insertContent(content, options = { triggerOnChange: true }) {
            if (content) {
                this.plugins.forEach(plugin => {
                    plugin.insertContent(content, options);
                });
            }
        },
        removeContent(selector, options) {
            this.plugins.forEach(plugin => {
                plugin.removeContent(selector, options);
            });
        },
        hasContent(selector) {
            return this.container.querySelectorAll(selector).length > 0;
        },
        removeText(text) {
            const node = findNodeWithText(this.container, text);
            if (node) {
                node.remove();
            }
        },
        focusBefore(selector) {
            let nodeToFocus = this.container.querySelector(selector)?.previousSibling;
            if (!nodeToFocus) {
                nodeToFocus = this.container.lastChild;
            }
            let textNode;
            do {
                textNode = findLastTextNode(nodeToFocus);
                nodeToFocus = nodeToFocus.previousSibling;
            } while (!textNode && nodeToFocus);

            if (textNode) {
                const selection = window.getSelection();
                const range = document.createRange();
                range.setStart(textNode, textNode.length);
                range.setEnd(textNode, textNode.length);
                selection.removeAllRanges();
                selection.addRange(range);
            } else {
                this.container.focus();
            }
        }
    }
};

function findLastTextNode(node) {
    if (node.nodeType === Node.TEXT_NODE && node.textContent) {
        return node;
    } else if (node.childNodes.length > 0) {
        for (let i = node.childNodes.length - 1; i >= 0; i--) {
            const textNode = findLastTextNode(node.childNodes[i]);
            if (textNode) {
                return textNode;
            }
        }
    }
}

export function findNodeWithText(node, text) {
    if (node.nodeType === Node.TEXT_NODE && node.textContent === text) {
        return node;
    } else if (node.childNodes.length > 0) {
        for (let i = 0; i < node.childNodes.length; i++) {
            const textNode = findNodeWithText(node.childNodes[i], text);
            if (textNode) {
                return textNode;
            }
        }
    }
}
