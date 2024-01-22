<script>
import { useExtensions } from "@bluemind/extensions.vue";

import BmIconButton from "../buttons/BmIconButton";
import BmDropdownItemButton from "../dropdown/BmDropdownItemButton";
import BmDropdown from "../dropdown/BmDropdown";
import BmToolbarIconDropdown from "./BmToolbarIconDropdown";

import { useToolbarContext } from "./toolbar";
import { computed, h, useAttrs, useListeners, useSlots } from "vue";

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
        overflownText: {
            type: String,
            required: true
        }
    },
    setup(props) {
        const { renderWebAppExtensions } = useExtensions();

        const attrs = useAttrs();
        const listeners = useListeners();
        const slots = useSlots();
        const { isInToolbar } = useToolbarContext();
        const extensions = computed(() => renderWebAppExtensions(props.extension));
        const options = computed(() => ({
            attrs,
            props: {
                icon: props.icon,
                overflownText: props.overflownText,
                text: props.overflownText
            },
            on: listeners,
            class: "bm-toolbar-icon-button"
        }));

        const buildDropdownWithExtensions = () => h(BmDropdown, { ...options.value }, [...extensions.value]);

        return function render() {
            if (isInToolbar.value) {
                return h(BmIconButton, {
                    ...options.value,
                    props: { ...options.value.props, extension: props.extension }
                });
            }
            if (extensions.value.length) {
                return buildDropdownWithExtensions();
            }
            return h(BmDropdownItemButton, options.value, props.overflownText);
        };
    }
};
</script>
