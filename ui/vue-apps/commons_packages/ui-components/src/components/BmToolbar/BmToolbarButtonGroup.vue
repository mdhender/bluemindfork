<script>
import { useAttrs, useListeners, useSlots, h, computed } from "vue";
import BmButtonGroup from "../buttons/BmButtonGroup";
import BmToolbarToggle from "./BmToolbarToggle";
import BmToolbarElement from "./BmToolbarElement";

export default {
    name: "BmToolbarButtonGroup",
    components: { BmToolbarElement, BmToolbarToggle },
    setup() {
        const slots = useSlots();
        const options = computed(() => ({
            attrs: useAttrs(),
            class: "bm-toolbar-button-group"
        }));

        return function render() {
            return h(BmToolbarElement, {
                scopedSlots: {
                    toolbar: () => h(BmButtonGroup, options.value, slots.default()),
                    menu: () => {
                        const items = slots.default().filter(node => node.tag);
                        return h("div", options.value, items);
                    }
                }
            });
        };
    }
};
</script>
