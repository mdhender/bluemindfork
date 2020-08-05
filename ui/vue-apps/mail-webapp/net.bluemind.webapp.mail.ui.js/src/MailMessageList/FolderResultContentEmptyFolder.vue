<template>
    <mail-message-list-empty
        v-if="currentFolder"
        v-bm-tooltip.viewport
        :image="emptyFolderIllustration"
        class="content-empty-folder"
        :title="currentFolder.name"
    >
        {{ $t("mail.folder") }}
        <mail-folder-icon
            :shared="currentMailbox.type == 'mailshares'"
            :folder="currentFolder"
            class="font-weight-bold"
        />
        {{ $t("mail.empty") }}
    </mail-message-list-empty>
</template>

<script>
import { BmTooltip } from "@bluemind/styleguide";
import { mapGetters } from "vuex";
import emptyFolderIllustration from "../../assets/empty-folder.png";
import MailFolderIcon from "../MailFolderIcon";
import MailMessageListEmpty from "./MailMessageListEmpty";

export default {
    name: "FolderResultContentEmptyFolder",
    components: {
        MailFolderIcon,
        MailMessageListEmpty
    },
    directives: { BmTooltip },
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

<style>
.content-empty-folder {
    word-break: break-all;
}
</style>
