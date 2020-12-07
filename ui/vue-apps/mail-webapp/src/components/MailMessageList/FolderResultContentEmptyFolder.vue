<template>
    <mail-message-list-empty v-if="currentFolder" :image="emptyFolderIllustration" class="content-empty-folder">
        {{ $t("mail.folder") }}
        <mail-folder-icon
            :shared="CURRENT_MAILBOX.type == 'mailshares'"
            :folder="currentFolder"
            class="font-weight-bold"
        />
        {{ $t("mail.empty") }}
    </mail-message-list-empty>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import emptyFolderIllustration from "../../../assets/empty-folder.png";
import MailFolderIcon from "../MailFolderIcon";
import MailMessageListEmpty from "./MailMessageListEmpty";
import { CURRENT_MAILBOX } from "~getters";

export default {
    name: "FolderResultContentEmptyFolder",
    components: {
        MailFolderIcon,
        MailMessageListEmpty
    },
    data() {
        return {
            emptyFolderIllustration
        };
    },
    computed: {
        ...mapState("mail", ["folders", "activeFolder"]),
        ...mapGetters("mail", { CURRENT_MAILBOX }),
        currentFolder() {
            return this.folders[this.activeFolder];
        }
    }
};
</script>

<style>
.content-empty-folder {
    word-break: break-all;
}
</style>
