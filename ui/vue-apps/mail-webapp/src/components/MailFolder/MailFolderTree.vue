<template>
    <div class="mail-folder-tree">
        <bm-button
            variant="inline-info-dark"
            class="collapse-tree-btn d-flex align-items-center pb-2 pt-3 border-0 pl-2 w-100"
            :aria-controls="id"
            :aria-expanded="isTreeExpanded"
            @click.stop="isTreeExpanded = !isTreeExpanded"
        >
            <bm-icon :icon="isTreeExpanded ? 'caret-down' : 'caret-right'" size="sm" class="bm-icon mr-2" />
            <slot name="title" />
        </bm-button>
        <bm-collapse :id="id" v-model="isTreeExpanded">
            <bm-tree
                :tree="tree"
                :selected="activeFolder"
                class="text-nowrap"
                :children-property="FOLDER_GET_CHILDREN"
                breakpoint="xl"
                @toggle="key => SET_FOLDER_EXPANDED({ ...folders[key], expanded: !folders[key].expanded })"
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
import { mapGetters, mapMutations, mapState } from "vuex";
import { BmButton, BmCollapse, BmIcon, BmTree } from "@bluemind/styleguide";
import { SET_FOLDER_EXPANDED } from "~/mutations";
import { FOLDER_GET_CHILDREN } from "~/getters";
import { MailRoutesMixin } from "~/mixins";
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
    mixins: [MailRoutesMixin],
    props: {
        tree: {
            type: Array,
            required: true
        },
        showInput: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            isTreeExpanded: true
        };
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
        ...mapMutations("mail", { SET_FOLDER_EXPANDED }),

        selectFolder(key) {
            this.$router.push(this.folderRoute(this.folders[key]));
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-folder-tree .mail-folder-input svg {
    margin-left: $sp-1;
}
</style>
