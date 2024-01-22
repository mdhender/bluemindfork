<script>
import { BmExtension, useExtensions } from "@bluemind/extensions.vue";
import BmButtonToolbar from "../buttons/BmButtonToolbar";
import BmIconDropdown from "../dropdown/BmIconDropdown";
import OverflownElements from "../../directives/OverflownElements";
import { computed, h, ref, useAttrs, useListeners, useSlots } from "vue";

const Toolbar = {
    extends: BmButtonToolbar,
    provide() {
        return { $context: "toolbar" };
    }
};

const Menu = {
    extends: BmIconDropdown,
    props: {
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
        return { $context: "menu" };
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
        const { renderWebAppExtensions, normalizeSlot } = useExtensions();
        const slots = useSlots();
        const attrs = useAttrs();
        const listeners = useListeners();

        const hidden = ref(0);
        const shown = ref(0);
        function overflown({ detail: nodes }) {
            hidden.value = nodes.reduce((count, node) => (node.overflows ? ++count : count), 0);
            shown.value = nodes.length - hidden.value - 1;
        }

        const extensions = computed(() => renderWebAppExtensions(props.extension));
        const menuExtensions = computed(() => renderWebAppExtensions(`${props.extension}.menu`));

        return function render() {
            const items = [...normalizeSlot(slots.default()), ...extensions.value];
            const menuEntries = [
                ...items.slice(items.length - hidden.value),
                ...normalizeSlot(slots.menu && slots.menu()),
                ...menuExtensions.value
            ];
            const toolbarEntries = [
                ...items.slice(0, shown.value),
                ...(menuEntries.length ? [h(Menu, { ref: "more", class: "overflow-menu" }, menuEntries)] : []),
                ...items.slice(shown.value)
            ];
            const classes = menuEntries.length ? "bm-toolbar overflow" : "bm-toolbar";

            return h(
                Toolbar,
                {
                    class: classes,
                    directives: [{ name: "overflown-elements" }],
                    on: { overflown, ...listeners },
                    attrs
                },
                toolbarEntries
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
