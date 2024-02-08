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
        alignRight: {
            type: Boolean,
            default: false
        },
        extension: {
            type: String,
            default: undefined
        },
        extensionId: {
            type: String,
            default: undefined
        },
        menuIcon: {
            type: String,
            default: undefined
        },
        menuIconVariant: {
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
        const extensions = computed(() => renderWebAppExtensions(props.extension, attrs, props.extensionId));
        const menuExtensions = computed(() =>
            renderWebAppExtensions(`${props.extension}.menu`, attrs, props.extensionId)
        );
        function overflown({ detail: nodes }) {
            let toHide = nodes.reduce((count, node) => (node.overflows ? ++count : count), 0);
            const hasMenu = nodes.some(node => node.element.className.includes("overflow-menu"));
            const size = hasMenu ? nodes.length - 1 : nodes.length;

            let toShow = size - toHide;
            if (toShow > props.maxItems) {
                toShow = props.maxItems;
                toHide = size - toShow;
            } else if (toShow < props.minItems) {
                toShow = props.minItems;
                toHide = size - toShow;
            }
            hidden.value = Math.max(toHide, 0);
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
                ...items.slice(shown.value),
                ...normalizeSlot(slots.menu && slots.menu()),
                ...menuExtensions.value
            ].filter(isNotComment);

            const buttonContentSlot = slots?.["menu-button"] ? () => slots["menu-button"]() : null;
            const menuButtonOptions = {
                class: "overflow-menu",
                props: {
                    icon: props.menuIcon,
                    variant: props.menuIconVariant,
                    size: "sm",
                    noCaret: !props.menuWithCaret
                },
                scopedSlots: {
                    "menu-button": buttonContentSlot
                }
            };
            const menuButton = menuEntries.length ? [h(BmToolbarMenu, menuButtonOptions, menuEntries)] : [];
            const overflowing = Math.min(items.length, props.maxItems);
            const toolbarEntries = [
                ...items.slice(0, shown.value),
                ...menuButton,
                ...items.slice(shown.value, overflowing)
            ];

            return h(
                BmToolbarInternal,
                {
                    class: { "bm-toolbar": true, overflow: menuEntries.length, right: props.alignRight },
                    directives: [{ name: "overflown-elements" }],
                    on: { overflown, ...listeners },
                    attrs
                },
                toolbarEntries
            );
        };
    }
};

function isNotComment(node) {
    return node.elm?.nodeType !== Node.COMMENT_NODE;
}
</script>

<style lang="scss">
@import "../../css/utils/variables";

.bm-toolbar {
    position: relative;
    display: flex;
    flex-wrap: nowrap !important;

    &.right {
        // Force hidden elements to overflow below the toolbar so that it can be sticked on the right
        // and force a height to constrain the toolbar to one line.
        flex-wrap: wrap !important;
        max-height: calc($icon-btn-height-lg + 2px);
    }

    .overflow-menu ~ * {
        visibility: hidden;
        order: 1000 !important;
    }
    .overflow-menu {
        order: 999;
        > button {
            height: 100%;
        }
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
