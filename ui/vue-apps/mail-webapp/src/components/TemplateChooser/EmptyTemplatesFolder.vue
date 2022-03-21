<template>
    <mail-conversation-list-empty :image="emptyFolderIllustration" class="content-empty-folder">
        {{ $t("mail.folder") }}
        <mail-folder-icon :folder="MY_TEMPLATES" class="font-weight-bold" />
        {{ $t("mail.empty") }}
    </mail-conversation-list-empty>
</template>

<script>
import { mapGetters } from "vuex";
import emptyFolderIllustration from "~/../assets/empty-folder.png";
import MailFolderIcon from "../MailFolderIcon";
import MailConversationListEmpty from "../MailConversationList/MailConversationListEmpty";
import { MY_TEMPLATES } from "~/getters";

export default {
    name: "EmptyTemplatesFolder",
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
        ...mapGetters("mail", { MY_TEMPLATES })
    }
};
</script>

<style>
.content-empty-folder {
    word-break: break-all;
}
</style>
