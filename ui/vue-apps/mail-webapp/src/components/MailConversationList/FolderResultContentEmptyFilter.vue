<template>
    <mail-conversation-list-empty :image="image">
        <h3 class="d-inline text-center">
            <p v-if="CONVERSATION_LIST_UNREAD_FILTER_ENABLED">{{ $t("mail.list.unread.none") }}</p>
            <p v-if="CONVERSATION_LIST_FLAGGED_FILTER_ENABLED">{{ $t("mail.list.flagged.none") }}</p>
            <p>
                <router-link :to="$router.relative({ name: 'v:mail:home', params: { filter: null } }, $route)">
                    {{ $t("mail.list.filter.remove") }}
                </router-link>
            </p>
        </h3>
    </mail-conversation-list-empty>
</template>

<script>
import { mapGetters } from "vuex";
import emptyFolderIllustrationUnreadFilter from "../../../assets/empty-folder-unread-filter.png";
import emptyFolderIllustrationFlaggedFilter from "../../../assets/empty-folder-flagged-filter.png";
import MailConversationListEmpty from "./MailConversationListEmpty";
import { CONVERSATION_LIST_UNREAD_FILTER_ENABLED, CONVERSATION_LIST_FLAGGED_FILTER_ENABLED } from "~/getters";

export default {
    name: "FolderResultContentEmptyFilter",
    components: {
        MailConversationListEmpty
    },
    data() {
        return {
            emptyFolderIllustrationUnreadFilter,
            emptyFolderIllustrationFlaggedFilter
        };
    },
    computed: {
        ...mapGetters("mail", { CONVERSATION_LIST_UNREAD_FILTER_ENABLED, CONVERSATION_LIST_FLAGGED_FILTER_ENABLED }),
        image() {
            if (this.CONVERSATION_LIST_UNREAD_FILTER_ENABLED) {
                return this.emptyFolderIllustrationUnreadFilter;
            }
            if (this.CONVERSATION_LIST_FLAGGED_FILTER_ENABLED) {
                return this.emptyFolderIllustrationFlaggedFilter;
            }
            return null;
        }
    }
};
</script>
