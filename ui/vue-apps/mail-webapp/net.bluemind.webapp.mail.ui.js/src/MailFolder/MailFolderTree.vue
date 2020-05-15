<template>
    <div class="mail-folder-tree">
        <bm-button
            variant="link"
            class="collapse-mailbox-btn d-flex align-items-center pb-2 pt-3 border-0 pl-1 w-100"
            aria-controls="collapse-mailbox"
            :aria-expanded="isMailboxExpanded"
            @click="isMailboxExpanded = !isMailboxExpanded"
        >
            <bm-icon :icon="isMailboxExpanded ? 'caret-down' : 'caret-right'" size="sm" class="bm-icon mr-2" />
            <span class="font-weight-bold">{{ mailboxEmail }}</span>
        </bm-button>
        <bm-collapse id="collapse-mailbox" v-model="isMailboxExpanded">
            <bm-tree
                :value="tree.my"
                :selected="currentFolderKey"
                node-id-key="key"
                class="text-nowrap"
                breakpoint="xl"
                @expand="expandFolder"
                @collapse="collapseFolder"
                @select="selectFolder"
            >
                <template v-slot="f">
                    <mail-folder-item :folder="f.value" />
                </template>
            </bm-tree>
            <mail-folder-input class="pl-4" @submit="add" />
        </bm-collapse>
        <bm-button
            v-if="mailshares.length > 0"
            variant="link"
            class="collapse-mailbox-btn d-flex align-items-center pb-2 pt-3 border-0 pl-1 w-100"
            aria-controls="collapse-mailbox"
            :aria-expanded="areMailsharesExpanded"
            @click="areMailsharesExpanded = !areMailsharesExpanded"
        >
            <bm-icon :icon="areMailsharesExpanded ? 'caret-down' : 'caret-right'" size="sm" class="bm-icon mr-2" />
            <span class="font-weight-bold">{{ $t("common.mailshares") }}</span>
        </bm-button>
        <bm-collapse id="collapse-mailshares" v-model="areMailsharesExpanded">
            <bm-tree
                :value="tree.mailshares"
                :selected="currentFolderKey"
                node-id-key="key"
                class="text-nowrap"
                breakpoint="xl"
                @expand="expandFolder"
                @collapse="collapseFolder"
                @select="selectFolder"
            >
                <template v-slot="f">
                    <mail-folder-item shared :folder="f.value" />
                </template>
            </bm-tree>
        </bm-collapse>
    </div>
</template>

<script>
import { BmButton, BmCollapse, BmIcon, BmTree } from "@bluemind/styleguide";
import { ItemUri } from "@bluemind/item-uri";
import { mapGetters, mapActions, mapState } from "vuex";
import injector from "@bluemind/inject";
import MailFolderInput from "../MailFolderInput";
import MailFolderItem from "./MailFolderItem";

export default {
    name: "MailFolderTree",
    components: {
        BmButton,
        BmCollapse,
        BmIcon,
        BmTree,
        MailFolderInput,
        MailFolderItem
    },
    data() {
        return {
            isMailboxExpanded: true,
            areMailsharesExpanded: true,
            mailboxEmail: injector.getProvider("UserSession").get().defaultEmail
        };
    },
    computed: {
        ...mapGetters("mail-webapp", ["tree", "mailshares", "my"]),
        ...mapGetters("mail-webapp/folders", ["getFolderByKey"]),
        ...mapState("mail-webapp", ["currentFolderKey"])
    },
    methods: {
        ...mapActions("mail-webapp", ["expandFolder", "collapseFolder", "createFolder"]),
        selectFolder(key) {
            this.$emit("toggle-folders");
            const folder = this.getFolderByKey(key);
            const mailboxUid = ItemUri.container(key);
            if (mailboxUid === this.my.mailboxUid) {
                this.$router.push({ name: "v:mail:home", params: { folder: folder.value.fullName } });
            } else {
                const root = this.mailshares.find(mailshare => mailshare.mailboxUid === mailboxUid).root;
                const prefix = folder.value.parentUid !== null ? root + "/" : "";
                this.$router.push({ name: "v:mail:home", params: { mailshare: prefix + folder.value.fullName } });
            }
        },
        add(newFolderName) {
            this.createFolder({
                value: {
                    name: newFolderName,
                    fullName: newFolderName,
                    path: newFolderName,
                    parentUid: null
                },
                editing: false,
                displayName: newFolderName
            });
        }
    }
};
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-folder-tree {
    button.collapse-mailbox-btn {
        color: $info-dark;
        text-decoration-line: none;
        border-bottom: 1px solid $light !important;
    }
}
</style>
