<template>
    <mail-message-list-empty :image="emptyFolderIllustration">
        <h3 class="d-inline">
            {{ $t("mail.folder") }}
                <mail-folder-icon
                    v-if="currentFolder"
                    :shared="currentMailbox.type == 'mailshare'"
                    :folder="currentFolder.value"
                    class="font-weight-bold"
                />
        </h3>
        <h3 class="text-center">{{ $t("mail.empty") }}</h3>
    </mail-message-list-empty>
</template>

<script>
import { mapGetters } from "vuex";
import emptyFolderIllustration from "../../assets/empty-folder.png";
import MailFolderIcon from "../MailFolderIcon";
import MailMessageListEmpty from "./MailMessageListEmpty";

export default {
    name: "MailMessageListEmptyFolder",
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
        ...mapGetters("mail-webapp", ["currentFolder", "currentMailbox"])
    }
};
</script>
