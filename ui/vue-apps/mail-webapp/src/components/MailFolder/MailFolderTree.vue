<template>
    <div class="mail-folder-tree">
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
                @toggle="key => SET_FOLDER_EXPANDED({ ...folders[key], expanded: !folders[key].expanded })"
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
import { SET_FOLDER_EXPANDED } from "~mutations";
import { CREATE_FOLDER } from "~actions";
import { MY_MAILBOX } from "~getters";

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
        ...mapGetters("mail", { MY_MAILBOX }),
        ...mapState("mail", ["folders", "activeFolder"])
    },
    methods: {
        ...mapActions("mail", { CREATE_FOLDER }),
        ...mapMutations("mail", { SET_FOLDER_EXPANDED }),
        add(name) {
            this.CREATE_FOLDER({ name, parent: null, mailbox: this.MY_MAILBOX });
        },
        selectFolder(key) {
            this.$emit("toggle-folders");
            const folder = this.folders[key];
            if (folder.mailboxRef.key === this.MY_MAILBOX.key) {
                this.$router.push({ name: "v:mail:home", params: { folder: folder.path } });
            } else {
                this.$router.push({ name: "v:mail:home", params: { mailshare: folder.path } });
            }
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
