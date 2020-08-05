<template>
    <mail-message-list-empty :image="image">
        <h3 class="d-inline text-center">
            <p v-if="messageFilter === 'unread'">{{ $t("mail.list.unread.none") }}</p>
            <p v-if="messageFilter === 'flagged'">{{ $t("mail.list.flagged.none") }}</p>
            <p>
                <router-link :to="$router.relative({ name: 'v:mail:home', params: { filter: null } }, $route)">
                    {{ $t("mail.list.filter.remove") }}
                </router-link>
            </p>
        </h3>
    </mail-message-list-empty>
</template>

<script>
import emptyFolderIllustrationUnreadFilter from "../../../assets/empty-folder-unread-filter.png";
import emptyFolderIllustrationFlaggedFilter from "../../../assets/empty-folder-flagged-filter.png";
import MailMessageListEmpty from "./MailMessageListEmpty";
import { mapState } from "vuex";

export default {
    name: "FolderResultContentEmptyFilter",
    components: {
        MailMessageListEmpty
    },
    data() {
        return {
            emptyFolderIllustrationUnreadFilter,
            emptyFolderIllustrationFlaggedFilter
        };
    },
    computed: {
        ...mapState("mail-webapp", ["messageFilter"]),
        image() {
            switch (this.messageFilter) {
                case "unread":
                    return this.emptyFolderIllustrationUnreadFilter;
                case "flagged":
                    return this.emptyFolderIllustrationFlaggedFilter;
                default:
                    return null;
            }
        }
    }
};
</script>
