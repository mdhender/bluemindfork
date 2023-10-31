import { ContentPosition } from "roosterjs-editor-types";
import { findNodeWithText } from "../mixins/InsertContentMixin";
import adaptNode from "./adaptNode";

export default class {
    constructor(vm) {
        this.vm = vm;
    }

    insertContent(node, { movable, tooltip }) {
        if (movable) {
            this.vm.$data.movableContent = adaptNode(node, tooltip, movable);
            const textNode = findNodeWithText(this.vm.$data.container, movable);
            if (textNode) {
                textNode.replaceWith(this.vm.$data.movableContent);
            } else {
                this.vm.$data.editor.insertNode(this.vm.$data.movableContent, { position: ContentPosition.DomEnd });
            }
            this.vm.onChange();
        }
    }

    removeContent(selector, options) {
        if (options && options.movable) {
            this.vm.$data.movableContent = null;
        }
    }
}
