import { ContentPosition } from "roosterjs-editor-types";

export default class {
    constructor(vm) {
        this.vm = vm;
    }

    insertContent(node, options) {
        if (!options.editable) {
            this.vm.$data.editor.insertNode(node, { position: ContentPosition.End });
            if (options.triggerOnChange !== false) {
                this.vm.onChange();
            }
        }
    }

    removeContent(selector) {
        const nodes = this.vm.$data.container.querySelectorAll(selector);
        if (nodes.length > 0) {
            nodes.forEach(node => this.vm.$data.editor.deleteNode(node));
            this.vm.onChange();
        }
    }
}
