<script>
import { BmExtension, useExtensions } from "@bluemind/extensions.vue";
import BmToolbarMenu from "./BmToolbarMenu";
import BmIconDropdown from "../dropdown/BmIconDropdown";
import OverflownElements from "../../directives/OverflownElements";
import { computed, h, ref, useAttrs, useListeners, useSlots, watchPostEffect } from "vue";
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
        },
        maxItems: {
            type: Number,
            default: Number.MAX_SAFE_INTEGER,
            validator: function (value) {
                return !value || value > 0;
            }
        },
        minItems: {
            type: Number,
            default: 0,
            validator: function (value) {
                return !value || value >= 0;
            }
        }
    },
    setup(props) {
        const { renderWebAppExtensions, normalizeSlot } = useExtensions();
        const slots = useSlots();
        const attrs = useAttrs();
        const listeners = useListeners();

        const hidden = ref(0);
        const shown = ref(0);

        const extensions = computed(() => renderWebAppExtensions(props.extension));
        const menuExtensions = computed(() => renderWebAppExtensions(`${props.extension}.menu`));

        function overflown({ detail: nodes }) {
            let toHide = Math.min(
                nodes.reduce((count, node) => (node.overflows ? ++count : count), 0),
                nodes.length - 1 // Never hide the menu button
            );

            let toShow = nodes.length - toHide - 1;
            if (toShow > props.maxItems) {
                toShow = props.maxItems;
                toHide = nodes.length - toShow - 1;
            } else if (toShow < props.minItems) {
                toShow = props.minItems;
                toHide = nodes.length - toShow - 1;
            }
            hidden.value = toHide;
            shown.value = toShow;
        }

        watchPostEffect(() => {
            if (props.maxItems < props.minItems) {
                throw new Error("maxItems should be greater than minItems");
            }
        });

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
