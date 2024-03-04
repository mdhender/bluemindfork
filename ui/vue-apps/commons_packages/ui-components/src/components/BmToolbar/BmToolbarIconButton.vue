<script>
import { computed, h, useAttrs, useListeners, useSlots } from "vue";
import BmIconButton from "../buttons/BmIconButton";
import BmDropdownItemButton from "../dropdown/BmDropdownItemButton";
import BmDropdown from "../dropdown/BmDropdown";
import BmToolbarElement from "./BmToolbarElement";

export default {
    name: "BmToolbarIconButton",
    inheritAttrs: false,
    props: {
        extension: {
            type: String,
            default: undefined
        },
        extensionId: {
            type: String,
            default: undefined
        },
        icon: {
            type: String,
            default: undefined
        },
        text: {
            type: String,
            default: undefined
        }
    },
    setup(props) {
        const attrs = useAttrs();
        const slots = useSlots();
        const listeners = useListeners();

        return function render() {
            const text = props.text || attrs.title;
            const options = {
                on: listeners,
                props: {
                    icon: props.icon,
                    text
                },
                attrs,
                class: "bm-toolbar-icon-button",
                scopedSlots: slots
            };

            return h(BmToolbarElement, {
                props,
                attrs,
                scopedSlots: {
                    toolbar: () => h(BmIconButton, { ...options, attrs, props }),
                    menu: () => h(BmDropdownItemButton, { ...options, attrs }, text),
                    "menu-with-extensions": ({ extensions }) =>
                        h(BmDropdown, { ...options, attrs: { ...attrs, variant: undefined, split: true } }, extensions)
                }
            });
        };
    }
};
</script>
