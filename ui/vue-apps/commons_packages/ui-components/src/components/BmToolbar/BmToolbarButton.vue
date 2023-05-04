<script>
import BmButton from "../buttons/BmButton.vue";
import BmDropdownItemButton from "../dropdown/BmDropdownItemButton.vue";
import BmToolbarDropdown from "./BmToolbarDropdown.vue";
import { getExtensionsContent, useToolbarContext } from "./toolbar";
import { computed, h, useAttrs, useListeners, useSlots } from "vue";

export default {
    name: "BmToolbarButton",
    props: {
        extension: {
            type: String,
            default: undefined
        }
    },
    setup(props) {
        const attrs = useAttrs();
        const listeners = useListeners();
        const slots = useSlots();
        const { isInToolbar } = useToolbarContext();
        console.log(attrs, slots.default);
        const extensions = computed(() => (props.extension ? getExtensionsContent(props.extension) : []));
        const options = computed(() => ({ attrs: { ...attrs }, on: listeners }));

        const buildDropdownWithExtensions = () =>
            h(BmToolbarDropdown, { ...options.value, attrs: { ...options.value.attrs, split: true } }, [
                h("template", { slot: "button-content" }, slots.default()),
                ...extensions.value
            ]);

        return function render() {
            if (extensions.value.length) {
                return buildDropdownWithExtensions();
            }

            if (isInToolbar.value) {
                return h(BmButton, options.value, slots.default());
            }
            return h(BmDropdownItemButton, options.value, slots.default());
        };
    }
};
</script>
