<template>
    <div class="mail-folder-tree">
        <bm-button
            variant="inline-neutral"
            class="collapse-tree-btn d-flex align-items-center pb-2 pt-3 border-0 pl-2 w-100"
            :aria-controls="id"
            :aria-expanded="expanded"
            @click.stop="$emit('toggle-tree')"
        >
            <bm-icon :icon="expanded ? 'caret-down' : 'caret-right'" size="sm" class="bm-icon mr-2" />
            <slot name="title" />
        </bm-button>
        <bm-collapse :id="id" :visible="expanded">
            <bm-tree
                :tree="tree"
                :selected="activeFolder"
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
import { BmButton, BmCollapse, BmIcon, BmTree } from "@bluemind/styleguide";
import { FOLDER_GET_CHILDREN } from "~/getters";
import { FolderMixin, MailRoutesMixin } from "~/mixins";
import DraggableMailFolderItem from "./DraggableMailFolderItem";

export default {
    name: "MailFolderTree",
    components: {
        BmButton,
        BmCollapse,
        BmIcon,
        BmTree,
        DraggableMailFolderItem
    },
    mixins: [FolderMixin, MailRoutesMixin],
    props: {
        tree: {
            type: Array,
            required: true
        },
        expanded: {
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
