<template>
    <mail-message-list-empty
        v-bm-tooltip.ds500.viewport
        :image="emptyFolderIllustration"
        class="content-empty-folder"
        :title="currentFolder.value.name"
    >
        {{ $t("mail.folder") }}
        <mail-folder-icon
            v-if="currentFolder"
            :shared="currentMailbox.type == 'mailshare'"
            :folder="currentFolder.value"
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
