import { findNodeWithText } from "../mixins/InsertContentMixin";

export const NON_EDITABLE_CONTENT_DROP_ID = "moving-non-editable-content";

export default class {
    constructor(vm) {
        this.vm = vm;
    }

    insertContent(node, { editable, placeholder, tooltip }) {
        if (editable) {
            this.vm.$data.nonEditableContent = adaptNonEditable(node, tooltip);
            const textNode = findNodeWithText(this.vm.$data.container, placeholder);
            if (textNode) {
                textNode.replaceWith(this.vm.$data.nonEditableContent);
            } else {
                this.vm.$data.container.appendChild(this.vm.$data.nonEditableContent);
            }
            this.vm.onChange();
        }
    }

    removeContent(selector, options) {
        if (options?.editable) {
            this.vm.$data.nonEditableContent = null;
        }
    }
}

function adaptNonEditable(node, tooltip) {
    node.setAttribute("contenteditable", "false");
    node.setAttribute("draggable", "true");
    node.setAttribute("style", "user-select: none; cursor: grab; cursor: -webkit-grab;");
    node.setAttribute("title", tooltip);
    node.querySelectorAll("img, a").forEach(imgNode => {
        imgNode.setAttribute("draggable", "false");
    });
    node.querySelectorAll("a").forEach(imgNode => {
        imgNode.setAttribute("target", "_blank");
    });
    node.addEventListener("dragstart", event => {
        event.dataTransfer.dropEffect = "move";
        event.dataTransfer.effectAllowed = "move";
        event.dataTransfer.setData(NON_EDITABLE_CONTENT_DROP_ID, "true");
    });

    // for accessibility purpose
    node.setAttribute("tabindex", "0");
    node.addEventListener("keydown", event => {
        event.preventDefault(); // avoid scrolling
        if (event.keyCode === 38) {
            // arrow up
            if (node.previousSibling) {
                if (node.previousSibling.nodeName === "DIV" && node.previousSibling.childNodes.length > 0) {
                    node.previousElementSibling.appendChild(node);
                } else {
                    node.previousSibling.before(node);
                }
            } else if (node.parentElement?.previousElementSibling) {
                node.parentElement.previousElementSibling.appendChild(node);
            }
            node.focus();
        } else if (event.keyCode === 40) {
            // arrow down
            if (node.nextSibling) {
                if (node.nextSibling.nodeName === "DIV" && node.nextSibling.childNodes.length > 0) {
                    node.nextSibling.prepend(node);
                } else {
                    node.nextSibling.after(node);
                }
            } else if (node.parentElement?.nextElementSibling) {
                node.parentElement.nextElementSibling.prepend(node);
            }
            node.focus();
        }
    });
    return node;
}
