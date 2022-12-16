<script>
import BmButton from "../buttons/BmButton.vue";
import BmDropdown from "../dropdown/BmDropdown.vue";
import BmDropdownItemButton from "../dropdown/BmDropdownItemButton.vue";
export default {
    name: "BmToolbarButton",
    inject: ["$context"],
    components: { BmButton, BmDropdown, BmDropdownItemButton },

    computed: {
        context() {
            return this.$context?.$options.name === "BButtonToolbar" ? "toolbar" : "menu";
        }
    },
    render(h) {
        let options = { attrs: { ...this.$attrs }, on: { ...this.$listeners } };
        let component;
        if (this.context === "toolbar") {
            component = h("bm-button", options, this.$slots.default);
        } else {
            component = h("bm-dropdown-item-button", options, this.$slots.default);
        }
        return component;
    }
};
</script>

BmButton
