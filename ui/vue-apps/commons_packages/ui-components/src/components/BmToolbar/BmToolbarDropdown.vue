<script>
import { useSlots, useListeners, useAttrs, h, computed } from "vue";
import BmDropdown from "../dropdown/BmDropdown.vue";
import BmIconDropdown from "../dropdown/BmIconDropdown.vue";
import { getExtensionsContent } from "./toolbar";

export default {
    name: "BmToolbarDropdown",
    props: {
        extension: {
            type: String,
            default: undefined
        }
    },
    setup(props) {
        const slots = useSlots();
        const attrs = useAttrs();
        const listeners = useListeners();

        const options = { attrs, on: listeners };

        const extensions = computed(() => (props.extension ? getExtensionsContent(props.extension) : []));
        const children = computed(() => [...slots.default(), ...extensions.value]);

        return function render() {
            return h(BmDropdown, { ...options, scopedSlots: { ...slots, default: undefined } }, children.value);
        };
    }
};
</script>
