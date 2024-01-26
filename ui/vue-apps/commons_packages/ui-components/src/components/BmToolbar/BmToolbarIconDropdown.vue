<script>
import { useSlots, useListeners, useAttrs, h, computed } from "vue";
import BmDropdown from "../dropdown/BmDropdown";
import BmIconDropdown from "../dropdown/BmIconDropdown.vue";
import BmToolbarElement from "./BmToolbarElement";

export default {
    name: "BmToolbarIconDropdown",
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
        const slots = useSlots();

        return function render() {
            const { extension, ...childProps } = props;
            const options = {
                attrs: useAttrs(),
                props: childProps,
                on: useListeners(),
                class: "bm-toolbar-icon-dropdown"
            };
            return h(BmToolbarElement, {
                props,
                scopedSlots: {
                    toolbar: () => h(BmIconDropdown, { ...options, props }, slots.default()),
                    menu: () => h(BmDropdown, { ...options }, slots.default()),
                    "menu-with-extensions": ({ extensions }) =>
                        h(BmDropdown, { ...options }, [slots.default(), ...extensions])
                }
            });
        };
    }
};
</script>
