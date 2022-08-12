<template>
    <div class="mail-folder-tree">
        <bm-button
            variant="text"
            class="collapse-tree-btn pl-2 w-100"
            size="sm"
            :aria-controls="id"
            :aria-expanded="!collapsed"
            :icon="collapsed ? 'caret-right' : 'caret-down'"
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
                <template v-slot="{ value }">
                    <draggable-mail-folder-item :folder="value" />
                </template>
            </bm-tree>
            <slot name="footer" />
        </bm-collapse>
    </div>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import { BmButton, BmCollapse, BmTree } from "@bluemind/styleguide";
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
@import "~@bluemind/styleguide/css/_variables";

.mail-folder-tree {
    .collapse-tree-btn {
        color: $primary-fg;
        &hover {
            color: $primary-fg-hi1;
        }
    }
    .mail-folder-input svg {
        margin-left: $sp-1;
    }
}
</style>
