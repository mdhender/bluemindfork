<script>
import { BmExtension } from "@bluemind/extensions.vue";
import BmButton from "../buttons/BmButton.vue";
import BmDropdown from "../dropdown/BmDropdown.vue";
import BmDropdownItemButton from "../dropdown/BmDropdownItemButton.vue";
import BmToolbarDropdown from "./BmToolbarDropdown.vue";
export default {
    name: "BmToolbarButton",
    inject: ["$context"],
    components: { BmButton, BmExtension, BmDropdown, BmDropdownItemButton, BmToolbarDropdown },
    props: {
        extension: {
            type: String,
            default: undefined
        }
    },
    computed: {
        context() {
            return this.$context?.$options.name === "BButtonToolbar" ? "toolbar" : "menu";
        }
    },
    render(h) {
        let options = { attrs: { ...this.$attrs }, on: { ...this.$listeners } };
        if (this.extension) {
            const extensions = normalizeSlot(h("bm-extension", { props: { id: "webapp", path: this.extension } }));
            if (extensions.length > 0) {
                return h(
                    "bm-toolbar-dropdown",
                    {
                        ...options,
                        attrs: { ...options.attrs, split: true }
                    },
                    [h("template", { slot: "button-content" }, this.$slots.default), ...extensions]
                );
            }
        }
        if (this.context === "toolbar") {
            return h("bm-button", options, this.$slots.default);
        }
        return h("bm-dropdown-item-button", options, this.$slots.default);
    }
};
function normalizeSlot(slot) {
    return (slot ? (Array.isArray(slot) ? slot : [slot]) : []).filter(vnode => Boolean(vnode.tag));
}
</script>
