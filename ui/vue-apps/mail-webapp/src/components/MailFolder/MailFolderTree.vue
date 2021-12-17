<template>
    <div class="mail-folder-tree">
        <bm-button
            variant="inline-info-dark"
            class="collapse-tree-btn d-flex align-items-center pb-2 pt-3 border-0 pl-2 w-100"
            :aria-controls="'collapse-tree-' + name"
            :aria-expanded="isTreeExpanded"
            @click.stop="isTreeExpanded = !isTreeExpanded"
        >
            <bm-icon :icon="isTreeExpanded ? 'caret-down' : 'caret-right'" size="sm" class="bm-icon mr-2" />
            <span class="font-weight-bold">{{ name }}</span>
        </bm-button>
        <bm-collapse :id="'collapse-tree-' + name" v-model="isTreeExpanded">
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
                    <mail-folder-item :folder-key="value.key" />
                </template>
            </bm-tree>
            <slot />
        </bm-collapse>
    </div>
</template>

<script>
import { mapGetters, mapMutations, mapState } from "vuex";
import { BmButton, BmCollapse, BmIcon, BmTree } from "@bluemind/styleguide";
import MailFolderItem from "./MailFolderItem";
import { SET_FOLDER_EXPANDED } from "~/mutations";
import { FOLDER_GET_CHILDREN } from "~/getters";
import { MailRoutesMixin } from "~/mixins";

export default {
    name: "MailFolderTree",
    components: {
        BmButton,
        BmCollapse,
        BmIcon,
        BmTree,
        MailFolderItem
    },
    mixins: [MailRoutesMixin],
    props: {
        name: {
            type: String,
            default: ""
        },
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
        ...mapState("mail", ["folders", "activeFolder"])
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
