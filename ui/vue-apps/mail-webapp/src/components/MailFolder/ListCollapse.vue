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
            <folder-tree-header :name="name" :mailbox="mailbox" />
        </bm-button>
        <bm-collapse :id="`collapse-${name}`" v-model="isExpanded">
            <slot />
        </bm-collapse>
    </div>
</template>
<script>
import { BmButton, BmCollapse } from "@bluemind/ui-components";
import FolderTreeHeader from "./FolderTreeHeader";

export default {
    name: "FilteredMailbox",
    components: {
        BmButton,
        BmCollapse,
        FolderTreeHeader
    },
    props: {
        name: {
            type: String,
            required: true
        },
        mailbox: {
            type: Object,
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
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/typography";
@import "~@bluemind/ui-components/src/css/utils/variables";

.folder-list-collapse {
    padding-top: $sp-3;
    & > .collapse {
        padding-bottom: $sp-6;
    }

    .collapse-tree-btn {
        padding-left: $sp-3 !important;
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

            @include from-lg {
                padding-right: $sp-3 + $sp-2;
            }

            .folder-tree-avatar {
                flex: none;
            }
            .folder-tree-name {
                flex: 1;
                min-width: 0;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;

                @include bold-tight;
            }
        }

        color: $primary-fg;
        &:hover {
            color: $primary-fg-hi1;
        }
    }
}
</style>
