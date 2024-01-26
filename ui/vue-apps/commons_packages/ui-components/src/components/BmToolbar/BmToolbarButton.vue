<script>
import { computed, h, useAttrs, useListeners, useSlots } from "vue";
import BmButton from "../buttons/BmButton";
import BmDropdownItemButton from "../dropdown/BmDropdownItemButton";
import BmDropdown from "../dropdown/BmDropdown";
import BmToolbarElement from "./BmToolbarElement";

export default {
    name: "BmToolbarButton",
    components: {
        BmButton,
        BmDropdown,
        BmDropdownItemButton,
        BmToolbarElement
    },
    inheritAttrs: false,
    props: {
        extension: {
            type: String,
            default: undefined
        }
    },
    setup(props) {
        const options = computed(() => ({
            attrs: useAttrs(),
            on: useListeners(),
            class: "bm-toolbar-button"
        }));
        const slots = useSlots();

        return function render() {
            const text = slots.default()?.[0].text.trim();

            return h(BmToolbarElement, {
                props,
                scopedSlots: {
                    toolbar: () => h(BmButton, { ...options.value, props, scopedSlots: slots }),
                    menu: () => h(BmDropdownItemButton, options.value, text),
                    "menu-with-extensions": ({ extensions }) =>
                        h(BmDropdown, { ...options.value, props: { text } }, extensions)
                }
            });
        };
    }
};
</script>
