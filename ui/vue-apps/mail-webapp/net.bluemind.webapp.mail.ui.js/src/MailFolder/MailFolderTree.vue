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
            <div
                class="pl-4 border-bottom new-folder d-flex align-items-center position-relative"
                :class="[
                    isNewFolderNameValid === true ? 'valid' : 'invalid border-danger',
                    newFolderIsFocused && isNewFolderNameValid === true ? 'border-primary' : ''
                ]"
            >
                <bm-icon :icon="newFolderIsFocused ? 'folder' : 'plus'" class="text-secondary" />
                <bm-form-input
                    ref="new"
                    v-model="newFolderName"
                    class="flex-fill"
                    :placeholder="$t('mail.folder.new.from_scratch')"
                    type="text"
                    reset
                    @focus="newFolderIsFocused = true"
                    @focusout="add()"
                    @keydown.enter="add()"
                    @keydown.esc="newFolderName = ''"
                    @reset="newFolderName = ''"
                />
                <bm-notice
                    v-if="isNewFolderNameValid !== true"
                    :text="isNewFolderNameValid"
                    class="position-absolute z-index-110 pr-2"
                />
            </div>
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
import { BmButton, BmCollapse, BmIcon, BmNotice, BmTree, BmFormInput } from "@bluemind/styleguide";
import { isFolderNameValid } from "@bluemind/backend.mail.store";
import { ItemUri } from "@bluemind/item-uri";
import { mapGetters, mapActions, mapState } from "vuex";
import injector from "@bluemind/inject";
import MailFolderItem from "./MailFolderItem";

export default {
    name: "MailFolderTree",
    components: {
        BmButton,
        BmCollapse,
        BmFormInput,
        BmIcon,
        BmNotice,
        BmTree,
        MailFolderItem
    },
    data() {
        return {
            isMailboxExpanded: true,
            areMailsharesExpanded: true,
            mailboxEmail: injector.getProvider("UserSession").get().defaultEmail,
            newFolderName: "",
            newFolderIsFocused: false
        };
    },
    computed: {
        ...mapGetters("mail-webapp", ["tree", "mailshares", "nextMessageKey", "my"]),
        ...mapGetters("mail-webapp/folders", ["getFolderByKey", "getFolderByPath"]),
        ...mapState("mail-webapp", ["currentFolderKey", "currentMessageKey"]),
        isNewFolderNameValid() {
            if (this.newFolderName !== "") {
                const currentMailbox = this.my.mailboxUid;

                const checkValidity = isFolderNameValid(this.newFolderName);
                if (checkValidity !== true) {
                    return this.$t("mail.actions.create.folder.invalid.character", {
                        character: checkValidity
                    });
                }

                if (this.getFolderByPath(this.newFolderName, currentMailbox)) {
                    return this.$t("mail.actions.create.folder.invalid.already_exist");
                }
            }
            return true;
        }
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
        add(parentFolderFullName) {
            console.log("focus out");
            if (this.isNewFolderNameValid === true && !!this.newFolderName) {
                this.newFolderIsFocused = false;
                const newFolderFullName = parentFolderFullName
                    ? parentFolderFullName + "/" + this.newFolderName
                    : this.newFolderName;
                this.createFolder(newFolderFullName).then(() => (this.newFolderName = ""));
            }
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

    .new-folder {
        input {
            border: none;
            padding-left: $sp-1;
        }
        .bm-notice {
            top: 33px;
        }
        &.valid {
            .fa-folder {
                color: $primary !important;
            }
        }
        &.invalid {
            .fa-folder {
                color: $danger !important;
            }
            input {
                color: $danger;
            }
        }
    }
}
</style>
