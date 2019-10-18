<template>
    <div class="h-100 bg-extra-light d-flex flex-column align-items-center justify-content-center">
        <div class="pb-5">
            <h3 class="d-inline">
                {{ $t("mail.folder") }} 
                <mail-folder-icon v-if="folder" :folder="folder" class="font-weight-bold" />
            </h3>
            <h3 class="text-center">{{ $t("mail.empty") }}</h3>
        </div>
        <div 
            class="empty-folder-illustration"
            :style="'background: url(' + emptyFolderIllustration +') no-repeat center top'"
        />
    </div>
</template>

<script>
import { mapGetters } from "vuex";
import emptyFolderIllustration from "../../assets/empty-folder.png";
import MailFolderIcon from "../MailFolderIcon";

export default {
    name: "MailMessageListEmptyFolder",
    components: {
        MailFolderIcon
    },
    data() {
        return {
            emptyFolderIllustration
        };
    },
    computed: {
        ...mapGetters("backend.mail/folders", ["currentFolder", "flat"]),
        folder() {
            return this.flat.filter(folder => folder.uid === this.currentFolder)[0];
        }
    }
};
</script>

<style scoped>
.empty-folder-illustration {
    height: 250px;
    width: 235px;
}
</style>