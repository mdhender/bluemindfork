<template>
    <div>
        <bm-button
            variant="inline-info-dark"
            class="collapse-tree-btn d-flex align-items-center pb-2 pt-3 border-0 pl-2 w-100"
            :aria-controls="'collapse-tree-' + collapseName"
            :aria-expanded="isTreeExpanded"
            @click="isTreeExpanded = !isTreeExpanded"
        >
            <bm-icon :icon="isTreeExpanded ? 'caret-down' : 'caret-right'" size="sm" class="bm-icon mr-2" />
            <span class="font-weight-bold">{{ collapseName }}</span>
        </bm-button>
        <bm-collapse :id="'collapse-tree-' + collapseName" v-model="isTreeExpanded">
            <bm-tree
                :tree="tree"
                :selected="activeFolder"
                node-id-key="key"
                class="text-nowrap"
                breakpoint="xl"
                @toggle="toggle"
                @select="selectFolder"
            >
                <template v-slot="{ value }">
                    <mail-folder-item :folder-key="value.key" />
                </template>
            </bm-tree>
            <mail-folder-input v-if="showInput" class="pl-4" @submit="add" />
        </bm-collapse>
    </div>
</template>

<script>
import { mapGetters, mapActions, mapMutations, mapState } from "vuex";
import { BmButton, BmCollapse, BmIcon, BmTree } from "@bluemind/styleguide";
import MailFolderInput from "../MailFolderInput";
import MailFolderItem from "./MailFolderItem";
import { TOGGLE_FOLDER } from "../../store/folders/mutations";

export default {
    name: "MailFolderMyMailbox",
    components: {
        BmButton,
        BmCollapse,
        BmIcon,
        BmTree,
        MailFolderInput,
        MailFolderItem
    },
    props: {
        tree: {
            type: Array,
            required: true
        },
        collapseName: {
            type: String,
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
        ...mapGetters("mail", ["MY_MAILBOX_KEY"]),
        ...mapState("mail", ["folders", "activeFolder"])
    },
    methods: {
        ...mapActions("mail-webapp", ["createFolder"]),
        ...mapMutations("mail", [TOGGLE_FOLDER]),
        add(newFolderName) {
            const folder = {
                name: newFolderName,
                path: newFolderName,
                parent: null
            };
            this.createFolder({ folder, mailboxUid: this.MY_MAILBOX_KEY });
        },
        selectFolder(key) {
            this.$emit("toggle-folders");
            const folder = this.folders[key];
            if (folder.mailbox === this.MY_MAILBOX_KEY) {
                this.$router.push({ name: "v:mail:home", params: { folder: folder.path } });
            } else {
                this.$router.push({ name: "v:mail:home", params: { mailshare: folder.path } });
            }
        },

        toggle(folderKey) {
            this.TOGGLE_FOLDER(folderKey);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-folder-tree {
    button.collapse-tree-btn {
        border-bottom: 1px solid $light !important;
    }
}
</style>
