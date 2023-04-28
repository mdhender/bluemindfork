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
    computed: {
        context() {
            return this.$context?.$options.name === "BButtonToolbar" ? "toolbar" : "menu";
        }
    },
    render(h) {
        const options = { attrs: { ...this.$attrs }, on: { ...this.$listeners }, scopedSlots: this.$scopedSlots };

        if (this.context === "toolbar") {
            return h("bm-dropdown", options);
        } else {
            const children = [h("bm-dropdown-divider")];
            if (this.$attrs.split !== undefined) {
                const content = this.$scopedSlots["button-content"]
                    ? this.$scopedSlots["button-content"]()
                    : this.$attrs.text;
                children.push(h("bm-dropdown-item-button", { attrs: options.attrs, on: options.on }, content));
            }
            children.push(...this.$scopedSlots.default());
            children.push(h("bm-dropdown-divider"));
            return h("bm-dropdown-group", children);
        }
    }
};
</script>
