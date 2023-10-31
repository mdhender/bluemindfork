<script>
import { BModal } from "bootstrap-vue";
import BmModalHeader from "./BmModalHeader";
import BmButton from "../buttons/BmButton";

export default {
    name: "BmModal",
    components: { BmModalHeader, BmButton },
    extends: BModal,
    props: {
        variant: {
            type: String,
            default: "basic",
            validator(value) {
                return ["basic", "advanced"].includes(value);
            }
        },
        height: {
            type: String,
            default: null,
            validator(value) {
                return [null, "sm", "md", "lg"].includes(value);
            }
        },
        centered: { type: Boolean, default: true },
        okVariant: { type: String, default: "fill-accent" },
        okIcon: { type: String, default: null },
        cancelVariant: { type: String, default: "text" },
        cancelIcon: { type: String, default: null }
    },
    computed: {
        dialogClasses() {
            return BModal.options.computed.dialogClasses
                .call(this)
                .concat(this.variant === "basic" ? [] : ["modal-dialog-advanced"])
                .concat(this.height === null ? [] : [`modal-height-${this.height}`]);
        }
    },
    methods: {
        fillSlotIfEmpty(slotName, renderer) {
            if (!this.$slots[slotName] && !this.$scopedSlots[slotName]) {
                this.$slots[slotName] = renderer();
            }
        },
        renderModalHeader(h) {
            return h(BmModalHeader, {
                props: { title: this.title },
                on: { close: this.onClose }
            });
        },
        renderModalCancel(h) {
            return h(
                BmButton,
                {
                    staticClass: "modal-cancel",
                    props: { variant: this.cancelVariant, icon: this.cancelIcon, disabled: this.cancelDisabled },
                    on: { click: this.onCancel }
                },
                [h("template", { slot: "default" }, this.cancelTitle)]
            );
        },
        renderModalOk(h) {
            return h(
                BmButton,
                {
                    staticClass: "modal-ok",
                    props: { variant: this.okVariant, icon: this.okIcon, disabled: this.okDisabled },
                    on: { click: this.onOk }
                },
                [h("template", { slot: "default" }, this.okTitle)]
            );
        },
        renderModalFooter(h) {
            return this.okOnly ? [this.renderModalOk(h)] : [this.renderModalCancel(h), this.renderModalOk(h)];
        }
    },
    render(h) {
        this.fillSlotIfEmpty("modal-header", () => this.renderModalHeader(h));
        this.fillSlotIfEmpty("modal-footer", () => this.renderModalFooter(h));
        return BModal.options.render.call(this, h);
    }
};
</script>
