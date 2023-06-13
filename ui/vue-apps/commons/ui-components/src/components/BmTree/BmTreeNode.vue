<template>
    <div class="bm-tree-node" @keydown.prevent.stop.right="doExpand" @keydown.prevent.stop.left="doCollapse">
        <div
            role="treeitem"
            :aria-expanded="hasChildren ? (isExpanded ? 'true' : 'false') : false"
            :aria-level="level + 1"
            :class="[id === selected ? 'bm-tree-node-active' : '', 'bm-tree-node-level-' + indentLevel]"
            class="bm-tree-node-content d-flex"
            data-browse
            @click="$emit('click', id)"
            @keydown.enter="$emit('click', id)"
            @focus="$emit('select', value)"
        >
            <bm-button-expand
                :style="[hasChildren ? { visibility: 'visible' } : { visibility: 'hidden' }]"
                :expanded="isExpanded"
                size="sm"
                aria-hidden="true"
                tabindex="-1"
                @click.stop="toggle"
            />
            <slot :value="value" />
        </div>
        <bm-collapse v-if="hasChildren && isExpanded" :id="makeUniq('children')" role="group" :visible="isExpanded">
            <bm-tree-node
                v-for="(child, index) in children"
                :key="childId(child)"
                :value="child"
                :id-property="idProperty"
                :level="level + 1"
                :selected="selected"
                :aria-setsize="children.length"
                :aria-posinset="index + 1"
                :is-expanded-property="isExpandedProperty"
                :has-children-property="hasChildrenProperty"
                :children-property="childrenProperty"
                @expand="childId => $emit('expand', childId)"
                @collapse="childId => $emit('collapse', childId)"
                @click="childId => $emit('click', childId)"
                @select="$emit('select', $event)"
            >
                <template v-slot="wrapIt">
                    <slot :value="wrapIt.value" />
                </template>
            </bm-tree-node>
        </bm-collapse>
    </div>
</template>

<script>
import BmButtonExpand from "../buttons/BmButtonExpand";
import BmToggle from "../../directives/BmToggle";
import BmCollapse from "../BmCollapse";
import MakeUniq from "../../mixins/MakeUniq";
import iteratee from "lodash.iteratee";

const maxIndentLevel = 5;

export default {
    name: "BmTreeNode",
    components: { BmButtonExpand, BmCollapse },
    directives: { BmToggle },
    mixins: [MakeUniq],
    props: {
        value: { type: Object, required: true },
        level: { type: Number, default: 0 },
        selected: { type: [String, Number], required: true },
        idProperty: { type: [String, Function], default: "key" },
        isExpandedProperty: { type: [String, Function, Object], default: "expanded" },
        childrenProperty: { type: [String, Function], required: true },
        hasChildrenProperty: { type: [String, Function, Object, Number], default: undefined }
    },
    data() {
        return { isExpanded: undefined, id: undefined, children: undefined, hasChildren: undefined, unwatchers: {} };
    },
    computed: {
        indentLevel() {
            return Math.min(this.level, maxIndentLevel);
        }
    },
    created() {
        this.addIterateeWatcher("isExpandedProperty", expanded => (this.isExpanded = expanded));
        this.addIterateeWatcher("idProperty", identifier => (this.id = identifier));
        this.addIterateeWatcher("childrenProperty", children => {
            this.children = children;
            if (this.hasChildrenProperty === undefined) {
                this.hasChildren = !!children?.length;
            }
        });
        this.addIterateeWatcher("hasChildrenProperty", hasChildren => (this.hasChildren = hasChildren));
    },
    destroyed() {
        Object.values(this.unwatchers).forEach(unwatch => unwatch());
    },
    methods: {
        addIterateeWatcher(propName, fn) {
            if (this[propName] != undefined) {
                this.unwatchers[propName] && this.unwatchers[propName]();
                this.unwatchers[propName] = this.$watch(() => iteratee(this[propName])(this.value), fn, {
                    immediate: true
                });
            }
        },
        toggle() {
            this.isExpanded = !this.isExpanded;
            if (this.isExpanded) {
                this.$emit("expand", this.id);
            } else {
                this.$emit("collapse", this.id);
            }
        },
        doExpand(event) {
            this.isExpanded = true;
            this.$emit("expand", this.id);
            event.stopImmediatePropagation();
        },
        doCollapse(event) {
            this.isExpanded = false;
            this.$emit("collapse", this.id);
            event.stopImmediatePropagation();
        },
        childId(value) {
            return iteratee(this.idProperty)(value);
        }
    }
};
</script>
<style lang="scss">
@import "../../css/utils/responsiveness.scss";
@import "../../css/utils/variables.scss";

$btn-width: 1.25rem;
@for $i from 0 through 5 {
    $levelSpacing: $spacer * $i * 0.8;
    .bm-tree-node-level-#{$i} {
        padding-left: $levelSpacing;

        & > span > * {
            margin-left: -#{$levelSpacing + $btn-width};
            padding-left: #{$levelSpacing + $btn-width};
        }
    }
}

.bm-tree-node {
    color: $neutral-fg;
}

.bm-tree-node-content {
    height: $tree-node-height-tactile;
    @include from-lg {
        height: $tree-node-height;
    }
    border-bottom: 1px solid $neutral-fg-lo3;
    cursor: pointer;
    &:hover {
        background-color: $neutral-bg-lo1;
        color: $neutral-fg-hi1;
    }

    .bm-button-expand {
        &.btn-icon-compact.btn-sm {
            height: 100%;
            width: $tree-expand-btn-width-tactile;
            @include from-lg {
                width: $tree-expand-btn-width;
            }
        }
    }

    .bm-button-expand + div {
        flex: 1;
        align-self: center;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }
}

.bm-tree-node-active {
    background-color: $secondary-bg-lo1;
    color: $neutral-fg-hi1;
    &:hover {
        background-color: $secondary-bg;
    }
}
</style>
