<script>
import { useAttrs, useListeners, useSlots, h, computed } from "vue";
import { useExtensions } from "@bluemind/extensions.vue";
import BmButtonGroup from "../buttons/BmButtonGroup";
import { useToolbarContext } from "./toolbar";
import BmDropdownItem from "../dropdown/BmDropdownItem";
import BmDropdownDivider from "../dropdown/BmDropdownDivider";
import BmToolbarButton from "./BmToolbarButton";
import BmToolbarToggle from "./BmToolbarToggle";
export default {
    name: "BmToolbarButtonGroup",
    components: { BmDropdownDivider, BmToolbarButton, BmToolbarToggle },
    setup(props) {
        const { renderWebAppExtensions } = useExtensions();
        const slots = useSlots();
        const attrs = useAttrs();

        const options = computed(() => ({
            attrs,
            props,
            class: "bm-toolbar-button-group"
        }));
        const { isInToolbar } = useToolbarContext();

        return function render() {
            if (isInToolbar.value) {
                return h(BmButtonGroup, options.value, slots.default());
            }
            const items = slots.default().filter(node => node.tag);
            return h("div", options.value, items);
        };
    }
};
</script>
