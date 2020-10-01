<template>
    <mail-message-list-empty :image="image">
        <h3 class="d-inline text-center">
            <p v-if="MESSAGE_LIST_UNREAD_FILTER_ENABLED">{{ $t("mail.list.unread.none") }}</p>
            <p v-if="MESSAGE_LIST_FLAGGED_FILTER_ENABLED">{{ $t("mail.list.flagged.none") }}</p>
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
import { mapGetters } from "vuex";

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
        ...mapGetters("mail", ["MESSAGE_LIST_UNREAD_FILTER_ENABLED", "MESSAGE_LIST_FLAGGED_FILTER_ENABLED"]),
        image() {
            if (this.MESSAGE_LIST_UNREAD_FILTER_ENABLED) {
                return this.emptyFolderIllustrationUnreadFilter;
            }
            if (this.MESSAGE_LIST_FLAGGED_FILTER_ENABLED) {
                return this.emptyFolderIllustrationFlaggedFilter;
            }
            return null;
        }
    }
};
</script>
