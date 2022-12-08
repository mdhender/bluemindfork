export default class {
    constructor(vm) {
        this.vm = vm;
    }

    insertContent(node, options) {
        if (!options.editable) {
            this.vm.$data.container.appendChild(node);
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
