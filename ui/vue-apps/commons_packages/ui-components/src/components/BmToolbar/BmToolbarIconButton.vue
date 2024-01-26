<script>
import { computed, h, useAttrs, useListeners } from "vue";
import BmIconButton from "../buttons/BmIconButton";
import BmDropdownItemButton from "../dropdown/BmDropdownItemButton";
import BmDropdown from "../dropdown/BmDropdown";
import BmToolbarElement from "./BmToolbarElement";

export default {
    name: "BmToolbarIconButton",
    props: {
        extension: {
            type: String,
            default: undefined
        },
        icon: {
            type: String,
            required: true
        },
        text: {
            type: String,
            required: true
        }
    },
    setup(props) {
        const attrs = useAttrs();
        const listeners = useListeners();
        const options = computed(() => ({
            attrs,
            on: listeners,
            props: {
                icon: props.icon,
                text: props.text
            },
            class: "bm-toolbar-icon-button"
        }));
        return function render() {
            return h(BmToolbarElement, {
                props,
                scopedSlots: {
                    toolbar: () => h(BmIconButton, { ...options.value, props }),
                    menu: () => h(BmDropdownItemButton, options.value, props.text),
                    "menu-with-extensions": ({ extensions }) => h(BmDropdown, options.value, extensions)
                }
            });
        };
    }
};
</script>
