import Vue from "vue";

const editors = {};
const BmRichEditorRegistry = Vue.extend({
    data: () => ({ editors }),
    methods: {
        register(name, editor) {
            this.$set(this.editors, name, editor);
        },
        unregister(name) {
            this.$delete(this.editors, name);
        },
        has(name) {
            return !!this.editors[name];
        },
        get(name) {
            return this.editors[name];
        }
    }
});

export default new BmRichEditorRegistry();
