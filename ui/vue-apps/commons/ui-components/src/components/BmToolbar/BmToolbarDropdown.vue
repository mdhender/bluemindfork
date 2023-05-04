<script>
import BmButton from "../buttons/BmButton.vue";
import BmDropdown from "../dropdown/BmDropdown.vue";
import BmDropdownItemButton from "../dropdown/BmDropdownItemButton.vue";
import BmDropdownGroup from "../dropdown/BmDropdownGroup";
import BmDropdownDivider from "../dropdown/BmDropdownDivider";
export default {
    name: "BmToolbarDropdown",
    inject: ["$context"],
    components: { BmButton, BmDropdown, BmDropdownDivider, BmDropdownGroup, BmDropdownItemButton },
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
        const { default: defaultSlot, ...scopedSlots } = this.$scopedSlots;
        const options = { attrs: { ...this.$attrs }, on: { ...this.$listeners } };
        const children = [...defaultSlot()];
        if (this.extension) {
            const extensions = normalizeSlot(h("bm-extension", { props: { id: "webapp", path: this.extension } }));
            if (extensions.length > 0) {
                children.push(...extensions);
            }
        }
        if (this.context === "toolbar") {
            return h("bm-dropdown", { ...options, scopedSlots: scopedSlots }, children);
        } else {
            if (this.$attrs.split !== undefined) {
                const content = this.$scopedSlots["button-content"]
                    ? this.$scopedSlots["button-content"]()
                    : this.$attrs.text;
                children.splice(0, 0, h("bm-dropdown-item-button", options, content));
            }
            children.splice(0, 0, h("bm-dropdown-divider"));
            children.push(h("bm-dropdown-divider"));
            return h("bm-dropdown-group", children);
        }
    }
};
function normalizeSlot(slot) {
    return (Array.isArray(slot) ? slot : slot ? [slot] : []).filter(vnode => Boolean(vnode.tag));
}
</script>
