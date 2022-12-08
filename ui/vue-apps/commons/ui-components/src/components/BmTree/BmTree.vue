<template>
    <div
        role="tree"
        class="bm-tree"
        @keydown.prevent.stop.down="focusNextItem"
        @keydown.prevent.stop.up="focusPreviousItem"
        @keyup.prevent.stop
    >
        <bm-tree-node
            v-for="(node, index) in tree"
            :key="index"
            :value="node"
            :selected="selected_"
            :indent-size="2"
            :id-property="idProperty"
            :aria-setsize="tree.length"
            :aria-posinset="index + 1"
            :is-expanded-property="isExpandedProperty"
            :has-children-property="hasChildrenProperty"
            :children-property="childrenProperty"
            @collapse="toggleNode($event)"
            @expand="toggleNode($event)"
            @click="onClick"
        >
            <template v-slot="wrap">
                <slot :value="wrap.value">
                    {{ wrap.value["label"] }}
                </slot>
            </template>
        </bm-tree-node>
    </div>
</template>

<script>
import BmTreeNode from "./BmTreeNode";
import BrowsableContainer from "../../mixins/BrowsableContainer";

export default {
    name: "BmTree",
    components: { BmTreeNode },
    mixins: [BrowsableContainer],
    props: {
        /**
         * Tree data. Structure looks like [{"uid": "1", "selected": true, "children" :[]}]
         */
        tree: {
            type: Array,
            required: true
        },
        /**
         * Node id. Must be unique.
         */
        idProperty: {
            type: [String, Function],
            required: false,
            default: "key"
        },
        selected: {
            type: [String, Number],
            default: ""
        },
        isExpandedProperty: {
            type: [String, Function, Object],
            required: false,
            default: "expanded"
        },
        childrenProperty: {
            type: [String, Function],
            required: false,
            default: "children"
        },
        hasChildrenProperty: {
            type: [String, Function, Object],
            required: false,
            default: undefined
        }
    },
    data() {
        return {
            selected_: this.selected,
            /** @Override BrowsableContainer#tabNavigation */
            tabNavigation: false
        };
    },
    watch: {
        selected() {
            this.selected_ = this.selected;
        }
    },
    methods: {
        toggleNode(nodeId) {
            this.$emit("toggle", nodeId);
            this.$forceUpdate(); // needed for tree not controlled by parent to make sur "focusables" is up to date
        },
        onClick(nodeId) {
            this.selected_ = nodeId;
            this.$emit("select", nodeId);
        },
        focusNextItem(e) {
            e.stopImmediatePropagation();
            this.focusNext();
        },
        focusPreviousItem(e) {
            e.stopImmediatePropagation();
            this.focusPrevious();
        }
    }
};
</script>
