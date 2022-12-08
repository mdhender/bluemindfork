<template>
    <div class="folder-list-collapse">
        <bm-button
            variant="text"
            class="collapse-tree-btn"
            size="sm"
            :aria-controls="`collapse-${name}`"
            :aria-expanded="isExpanded"
            :icon="isExpanded ? 'chevron' : 'chevron-right'"
            @click.stop="isExpanded = !isExpanded"
        >
            <div class="folder-tree-header">
                <slot name="avatar" />
                <div class="folder-tree-name">{{ name }}</div>
            </div>
        </bm-button>
        <bm-collapse :id="`collapse-${name}`" v-model="isExpanded">
            <slot />
        </bm-collapse>
    </div>
</template>
<script>
import { BmButton, BmCollapse } from "@bluemind/ui-components";

export default {
    name: "FilteredMailbox",
    components: {
        BmButton,
        BmCollapse
    },
    props: {
        name: {
            type: String,
            required: true
        }
    },
    data() {
        return {
            isExpanded: true
        };
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/_type";
@import "~@bluemind/ui-components/src/css/variables";

.folder-list-collapse {
    padding-top: $sp-3;
    & > .collapse {
        padding-bottom: $sp-6;
    }

    .collapse-tree-btn {
        padding-left: $sp-3 !important;
        padding-right: $sp-2 !important;
        gap: $sp-3 !important;
        justify-content: flex-start;
        width: 100%;
        &::before {
            display: none;
        }
        .folder-tree-header {
            display: flex;
            align-items: center;
            gap: $sp-3;

            .folder-tree-avatar {
                flex: none;
            }
            .folder-tree-name {
                flex: 1;
                min-width: 0;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;

                @extend %bold;
                letter-spacing: -0.04em;
            }
        }

        color: $primary-fg-hi1;
        &:hover {
            color: $highest;
        }
    }
}
</style>
