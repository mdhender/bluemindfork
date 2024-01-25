<script>
import { BmExtension, useExtensions } from "@bluemind/extensions.vue";
import BmToolbarMenu from "./BmToolbarMenu";
import BmIconDropdown from "../dropdown/BmIconDropdown";
import OverflownElements from "../../directives/OverflownElements";
import { computed, h, ref, useAttrs, useListeners, useSlots } from "vue";
import BmToolbarInternal from "./BmToolbarInternal";

export default {
    name: "BmToolbar",
    directives: { OverflownElements },
    props: {
        extension: {
            type: String,
            default: undefined
        },
        menuIcon: {
            type: String,
            default: undefined
        },
        menuWithCaret: {
            type: Boolean,
            default: false
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
            hidden.value = Math.min(
                nodes.reduce((count, node) => (node.overflows ? ++count : count), 0),
                nodes.length - 1
            );
            shown.value = nodes.length - hidden.value - 1;
        }

        const extensions = computed(() => renderWebAppExtensions(props.extension));
        const menuExtensions = computed(() => renderWebAppExtensions(`${props.extension}.menu`));

        return function render() {
            const defaultSlot = slots.default ? normalizeSlot(slots.default()) : [];
            const items = [...defaultSlot, ...extensions.value];
            const menuEntries = [
                ...items.slice(items.length - hidden.value),
                ...normalizeSlot(slots.menu && slots.menu()),
                ...menuExtensions.value
            ];
            const buttonContentSlot = slots?.["menu-button"] ? () => slots["menu-button"]() : null;
            const menuButtonOptions = {
                class: "overflow-menu",
                props: { icon: props.menuIcon, noCaret: !props.menuWithCaret },
                scopedSlots: {
                    "menu-button": buttonContentSlot
                }
            };
            const menuButton = menuEntries.length ? [h(BmToolbarMenu, menuButtonOptions, menuEntries)] : [];
            const toolbarEntries = [...items.slice(0, shown.value), ...menuButton, ...items.slice(shown.value)];
            const classes = menuEntries.length ? "bm-toolbar overflow" : "bm-toolbar";

            return h(
                BmToolbarInternal,
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

    .dropdown-menu {
        > li:first-child,
        > li:last-child {
            .dropdown-divider {
                display: none;
            }
        }
    }
}
</style>
