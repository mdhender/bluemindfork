<script>
import BmButton from "../buttons/BmButton.vue";
import BmDropdownItemButton from "../dropdown/BmDropdownItemButton.vue";
export default {
    name: "BmToolbarButton",
    inject: ["$context"],
    components: { BmButton, BmDropdownItemButton },

    computed: {
        context() {
            return this.$context?.$options.name === "BButtonToolbar" ? "toolbar" : "menu";
        }
    },
    render(h) {
        let options = { attrs: { ...this.$attrs }, on: { ...this.$listeners } };
        if (this.context === "toolbar") {
            return h("bm-button", options, this.$slots.default);
        }
        return h("bm-dropdown-item-button", options, this.$slots.default);
    }
};
</script>
