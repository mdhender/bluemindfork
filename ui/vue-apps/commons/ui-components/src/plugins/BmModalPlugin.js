import { ModalPlugin } from "bootstrap-vue";
import Vue from "vue";

export default {
    install: Vue => {
        Vue.use(ModalPlugin);
        const BvModal = new Vue()._bv__modal.constructor;
        /**
         *
         * @param {Component} Modal component. It must either extends BmModal or have a bm-modal as root component
         * @param {Object} props modal props values
         * @returns Modal instance
         */
        BvModal.prototype.open = function (component, props = {}) {
            if (typeof component === "string") {
                const name = component.replace(/\b(\w)/g, l => l.toUpperCase()).replace(/\W/g, "");
                component = this.$options.components[name] || Vue.options.components[name];
            }
            return showModal(this._vm, component, props);
        };
    }
};

function showModal($parent, component, props = {}) {
    const Modal = Vue.extend({
        extends: component,
        ...ModalBoxMixin
    });
    const modal = new Modal({
        propsData: props,
        parent: $parent
    });
    const div = document.createElement("div");
    document.body.appendChild(div);
    modal.$mount(div);
    return modal;
}

const ModalBoxMixin = {
    destroyed() {
        this.$el?.parentNode?.removeChild(this.$el);
    },
    mounted() {
        const modal = this.makeModal ? this : this.$children[0];
        const handleDestroy = async () => {
            await this.$nextTick();
            this.$destroy();
        };
        this.$parent.$once("hook:destroyed", handleDestroy);
        modal.$once("hidden", handleDestroy);
        if (this.$router && this.$route) {
            this.$once("hook:beforeDestroy", this.$watch("$router", handleDestroy));
        }
        modal.show();
    }
};
