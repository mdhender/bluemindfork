<script>
import { useSlots, useListeners, useAttrs, h, computed } from "vue";
import BmDropdown from "../dropdown/BmDropdown";
import BmIconDropdown from "../dropdown/BmIconDropdown";
import BmDropdownItemButton from "../dropdown/BmDropdownItemButton";
import BmToolbarElement from "./BmToolbarElement";

export default {
    name: "BmToolbarIconDropdown",
    inheritAttrs: false,
    props: {
        extension: {
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
        const slots = useSlots();

        return function render() {
            const attrs = useAttrs();
            const text = props.text || attrs.title;
            const options = {
                props: {
                    icon: props.icon,
                    text
                },
                on: useListeners(),
                class: "bm-toolbar-icon-dropdown",
                scopedSlots: slots
            };
            return h(BmToolbarElement, {
                props,
                scopedSlots: {
                    toolbar: () => h(BmIconDropdown, { ...options, attrs }, slots.default()),
                    menu: () =>
                        h(BmDropdown, { ...options, attrs: { ...attrs, variant: undefined } }, [slots.default()]),
                    "menu-with-extensions": ({ extensions }) =>
                        h(BmDropdown, { ...options, attrs: { ...attrs, variant: undefined } }, [
                            slots.default(),
                            ...extensions
                        ])
                }
            });
        };
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";

$icon-size: map-get($icon-sizes, "md");
.bm-toolbar-icon-dropdown {
    &.bm-dropdown .bm-icon {
        height: $icon-size !important;
        width: $icon-size !important;
    }
}
</style>
