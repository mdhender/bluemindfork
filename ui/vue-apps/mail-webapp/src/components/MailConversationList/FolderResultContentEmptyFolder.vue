<template>
    <div v-if="currentFolder" class="folder-result-content-empty-folder">
        {{ $t("mail.folder") }}
        <mail-folder-icon :mailbox="CURRENT_MAILBOX" :folder="currentFolder" class="font-weight-bold" />
        {{ $t("mail.empty") }}
    </div>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import MailFolderIcon from "../MailFolderIcon";
import { CURRENT_MAILBOX } from "~/getters";

export default {
    name: "FolderResultContentEmptyFolder",
    components: {
        MailFolderIcon
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
@import "~@bluemind/ui-components/src/css/_variables.scss";

.folder-result-content-empty-folder {
    display: flex;
    flex-wrap: wrap;
    gap: 0 base-px-to-rem(5);
    padding: $sp-8 $sp-5;
    justify-content: center;
}
</style>
