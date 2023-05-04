<script>
import BmButtonToolbar from "../buttons/BmButtonToolbar";
import BmIconDropdown from "../dropdown/BmIconDropdown";
import OverflownElements from "../../directives/OverflownElements";
import { getExtensionsContent, normalizeSlot } from "./toolbar";
import { computed, h, ref, useAttrs, useListeners, useSlots } from "vue";

const Toolbar = {
    extends: BmButtonToolbar,
    props: {
        context: {
            type: Object,
            default: () => {}
        }
    },
    provide() {
        return { $context: { ...this.context, renderContext: "toolbar" } };
    }
};

const Menu = {
    extends: BmIconDropdown,
    props: {
        context: {
            type: Object,
            default: () => {}
        },
        icon: {
            type: String,
            default: "3dots"
        },
        size: {
            type: String,
            default: "md"
        },
        noCaret: {
            type: Boolean,
            default: true
        }
    },
    provide() {
        return { $context: { ...this.context, renderContext: "menu" } };
    }
};

export default {
    name: "BmToolbar",
    directives: { OverflownElements },
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

        const hidden = ref(0);
        const shown = ref(0);
        function overflown({ detail: nodes }) {
            hidden.value = nodes.reduce((count, node) => (node.overflows ? ++count : count), 0);
            shown.value = nodes.length - hidden.value - 1;
        }

        const toolbarClasses = computed(() => (menuEntries.value.length ? "bm-toolbar overflow" : "bm-toolbar"));

        const extensions = computed(() => (props.extension ? getExtensionsContent(props.extension) : []));
        const menuExtensions = computed(() => (props.extension ? getExtensionsContent(`${props.extension}.menu`) : []));
        const items = computed(() => [...normalizeSlot(slots.default()), ...extensions.value]);
        const menuEntries = computed(() => [
            ...items.value.slice(items.value.length - hidden.value),
            ...normalizeSlot(slots.menu()),
            ...menuExtensions.value
        ]);
        const toolbarEntries = computed(() => [
            ...items.value.slice(0, shown.value),
            ...(menuEntries.value.length ? [h(Menu, { ref: "more", class: "overflow-menu" }, menuEntries.value)] : []),
            ...items.value.slice(shown.value)
        ]);

        return function render() {
            return h(
                Toolbar,
                {
                    class: toolbarClasses.value,
                    directives: [{ name: "overflown-elements" }],
                    on: { overflown, ...listeners },
                    attrs
                },
                toolbarEntries.value
            );
        };
    }
};
</script>

<style lang="scss">
.bm-toolbar {
    position: relative;
    display: flex;
    flex-wrap: nowrap !important;

    // FIXME : Add a more specific selector... is this even possible ?
    .overflow-menu ~ * {
        visibility: hidden;
        order: 1000 !important;
    }
    .overflow-menu {
        order: 999;
    }
}
</style>
