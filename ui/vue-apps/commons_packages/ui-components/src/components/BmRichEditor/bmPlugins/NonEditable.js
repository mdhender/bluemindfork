import { ContentPosition } from "roosterjs-editor-types";
import adaptNode from "./adaptNode";

export default class {
    constructor(vm) {
        this.vm = vm;
    }

    insertContent(node, { editable, movable, tooltip }) {
        if (!editable && !movable) {
            const adapted = adaptNode(node, tooltip);
            this.vm.$data.editor.insertNode(adapted, { position: ContentPosition.DomEnd });
            this.vm.onChange();
        }
    }

    removeContent() {}
}
