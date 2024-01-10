<script>
import { useSlots, useListeners, useAttrs, h, computed } from "vue";
import { useExtensions } from "@bluemind/extensions.vue";
import BmDropdown from "../dropdown/BmDropdown.vue";
import BmIconDropdown from "../dropdown/BmIconDropdown.vue";
import BmToolbarIconDropdown from "./BmToolbarIconDropdown.vue";

import { useToolbarContext } from "./toolbar";
import BmDropdownItemButton from "../dropdown/BmDropdownItem";

export default {
    name: "BmToolbarDropdown",
    props: {
        extension: {
            type: String,
            default: undefined
        },
        icon: {
            type: String,
            required: true
        },
        overflownText: {
            type: String,
            required: true
        }
    },
    setup(props) {
        const { renderWebAppExtensions } = useExtensions();
        const slots = useSlots();
        const attrs = useAttrs();
        const listeners = useListeners();

        const options = computed(() => ({
            attrs,
            props,
            on: listeners,
            class: "bm-toolbar-icon-dropdown"
        }));
        const { isInToolbar } = useToolbarContext();
        const className = "bm-toolbar-icon-dropdown";
        const extensions = computed(() => renderWebAppExtensions(props.extension));

        return function render() {
            if (isInToolbar.value) {
                return h(BmIconDropdown, { attrs, props, on: listeners, class: className }, slots.default());
            }
            return h(
                BmDropdown,
                {
                    attrs,
                    props: { ...props, extension: undefined, text: props.overflownText },
                    on: listeners,
                    class: className
                },
                [slots.default(), ...extensions.value]
            );
        };
    }
};
</script>
