<template>
    <mail-conversation-list-empty v-if="currentFolder" :image="emptyFolderIllustration" class="content-empty-folder">
        {{ $t("mail.folder") }}
        <mail-folder-icon :mailbox="CURRENT_MAILBOX" :folder="currentFolder" class="font-weight-bold px-3" />
        {{ $t("mail.empty") }}
    </mail-conversation-list-empty>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import emptyFolderIllustration from "../../../assets/empty-folder.png";
import MailFolderIcon from "../MailFolderIcon";
import MailConversationListEmpty from "./MailConversationListEmpty";
import { CURRENT_MAILBOX } from "~/getters";

export default {
    name: "FolderResultContentEmptyFolder",
    components: {
        MailFolderIcon,
        MailConversationListEmpty
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

<style lang="scss">
@import "~@bluemind/ui-components/src/css/mixins";

.content-empty-folder {
    & > div {
        white-space: nowrap;
    }
    .mail-folder-icon {
        min-width: 0;
        div {
            @include text-overflow;
        }
    }
}
</style>
