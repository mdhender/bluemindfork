<template>
    <div class="mail-folder-tree">
        <bm-button
            variant="text"
            class="collapse-tree-btn"
            size="sm"
            :aria-controls="id"
            :aria-expanded="!collapsed"
            :icon="collapsed ? 'chevron-right' : 'chevron'"
            @click.stop="$emit('toggle-tree')"
        >
            <slot name="title" />
        </bm-button>
        <bm-collapse :id="id" :visible="!collapsed">
            <bm-tree
                :tree="tree"
                :selected="activeFolder"
                :is-expanded-property="folder => $store.state.mail.folderList.expandedFolders.indexOf(folder.key) > -1"
                class="text-nowrap"
                :children-property="FOLDER_GET_CHILDREN"
                breakpoint="xl"
                @toggle="toggle"
                @select="selectFolder"
            >
                <template #default="{ value }">
                    <draggable-mail-folder-item :folder="value" />
                </template>
            </bm-tree>
            <slot name="footer" />
        </bm-collapse>
    </div>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import { BmButton, BmCollapse, BmTree } from "@bluemind/ui-components";
import { FOLDER_GET_CHILDREN } from "~/getters";
import { FolderMixin, MailRoutesMixin } from "~/mixins";
import DraggableMailFolderItem from "./DraggableMailFolderItem";

export default {
    name: "MailFolderTree",
    components: {
        BmButton,
        BmCollapse,
        BmTree,
        DraggableMailFolderItem
    },
    mixins: [FolderMixin, MailRoutesMixin],
    props: {
        tree: {
            type: Array,
            required: true
        },
        collapsed: {
            type: Boolean,
            required: true
        },
        showInput: {
            type: Boolean,
            default: false
        }
    },
    computed: {
        ...mapGetters("mail", { FOLDER_GET_CHILDREN }),
        ...mapState("mail", ["folders", "activeFolder"]),
        id() {
            const randomId = Math.floor(Math.random() * 100);
            return `collapse-tree-${randomId}`;
        }
    },
    methods: {
        selectFolder(key) {
            this.$router.push(this.folderRoute(this.folders[key]));
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/text.scss";
@import "~@bluemind/ui-components/src/css/utils/typography";
@import "~@bluemind/ui-components/src/css/utils/variables";

.mail-folder-tree {
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

    .mail-folder-input svg {
        margin-left: $sp-1;
    }

    .bm-button .bm-button-content {
        @include text-overflow;
    }
}
</style>
