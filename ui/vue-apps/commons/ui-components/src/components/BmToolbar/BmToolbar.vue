<script>
import { BmExtension } from "@bluemind/extensions.vue";
import BmButtonToolbar from "../buttons/BmButtonToolbar";
import BmIconDropdown from "../dropdown/BmIconDropdown";
import OverflownElements from "../../directives/OverflownElements";

const Toolbar = {
    extends: BmButtonToolbar,
    provide() {
        return { $context: this };
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
        //FIXME: beurk
        return { $context: this };
    }
};
export default {
    name: "BmToolbar",
    components: { BmButtonToolbar, BmExtension, BmIconDropdown },
    directives: { OverflownElements },
    provide() {
        return { $toolbar: this };
    },
    props: {
        extension: {
            type: String,
            default: undefined
        }
    },
    data: () => ({ hidden: 0, shown: 0 }),
    methods: {
        async overflow({ detail: nodes }) {
            this.hidden = nodes.reduce((count, node) => (node.overflows ? ++count : count), 0);
            this.shown = nodes.length - this.hidden - 1;
        }
    },
    render(h) {
        const classes = ["bm-toolbar"];
        const items = normalizeSlot(this.$slots.default);
        if (this.extension) {
            let extension = h("bm-extension", { props: { id: "webapp", path: this.extension } });
            items.push(...normalizeSlot(extension));
        }
        const entries = items.slice(items.length - this.hidden).concat(normalizeSlot(this.$slots.menu));
        if (this.extension) {
            let extension = h("bm-extension", { props: { id: "webapp", path: `${this.extension}.menu` } });
            entries.push(...normalizeSlot(extension));
        }
        if (entries.length > 0) {
            classes.push("overflow");
            let menu = h(Menu, { ref: "more", class: "overflow-menu" }, entries);
            items.splice(this.shown, 0, menu);
        }
        return h(
            Toolbar,
            {
                class: classes,
                directives: [{ name: "overflown-elements" }],
                on: { overflown: this.overflow, ...this.$listeners },
                attrs: { ...this.$attrs }
            },
            [...items]
        );
    }
};

function normalizeSlot(slot) {
    return (Array.isArray(slot) ? slot : slot ? [slot] : []).filter(vnode => Boolean(vnode.tag));
}
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
