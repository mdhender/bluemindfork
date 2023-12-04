<template>
    <i18n v-if="currentFolder" class="folder-result-content-empty-folder" :path="i18nPath">
        <template #folder>
            <mail-folder-icon :mailbox="CURRENT_MAILBOX" :folder="currentFolder" class="bold" />
        </template>
        <template #nbSubfolders>
            {{ nbSubfolders }}
        </template>
    </i18n>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import MailFolderIcon from "../MailFolderIcon";
import { CURRENT_MAILBOX, FOLDER_GET_CHILDREN } from "~/getters";

export default {
    name: "FolderResultContentEmptyFolder",
    components: {
        MailFolderIcon
    },
    computed: {
        ...mapState("mail", ["folders", "activeFolder"]),
        ...mapGetters("mail", { CURRENT_MAILBOX, FOLDER_GET_CHILDREN }),
        i18nPath() {
            switch (this.nbSubfolders) {
                case 0:
                    return "common.folder.empty";
                case 1:
                    return "common.folder.subfolder";
                default:
                    return "common.folder.subfolders";
            }
        },
        currentFolder() {
            return this.folders[this.activeFolder];
        },
        nbSubfolders() {
            return this.FOLDER_GET_CHILDREN(this.currentFolder).length;
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables.scss";

.folder-result-content-empty-folder {
    flex-wrap: wrap;
    gap: 0 base-px-to-rem(5);
    padding: $sp-8 $sp-5;
    display: inline-flex;
    justify-content: center;
    align-content: start;
}
</style>
