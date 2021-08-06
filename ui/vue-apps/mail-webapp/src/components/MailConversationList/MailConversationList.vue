<template>
    <section :aria-label="$t('mail.application.region.messagelist')" class="mail-conversation-list d-flex flex-column">
        <mail-conversation-list-header id="mail-conversation-list-header" />
        <search-result v-if="CONVERSATION_LIST_IS_SEARCH_MODE" class="flex-fill" />
        <folder-result v-else class="flex-fill" />
    </section>
</template>

<script>
import { mapGetters, mapState, mapActions } from "vuex";
import MailConversationListHeader from "./MailConversationListHeader";
import SearchResult from "./SearchResult";
import FolderResult from "./FolderResult";
import { CONVERSATION_MESSAGE_BY_KEY, CONVERSATION_LIST_IS_SEARCH_MODE, CONVERSATION_LIST_KEYS } from "~/getters";
import { FETCH_MESSAGE_METADATA, REFRESH_CONVERSATION_LIST_KEYS } from "~/actions";
import { PUSHED_FOLDER_CHANGES } from "../VueBusEventTypes";

export default {
    name: "MailConversationList",
    components: {
        MailConversationListHeader,
        SearchResult,
        FolderResult
    },
    computed: {
        ...mapGetters("mail", {
            CONVERSATION_MESSAGE_BY_KEY,
            CONVERSATION_LIST_IS_SEARCH_MODE,
            CONVERSATION_LIST_KEYS
        }),
        ...mapState("mail", ["activeFolder", "folders", "conversationList"]),
        ...mapState("session", { settings: ({ settings }) => settings.remote }),
        folder() {
            return this.folders[this.activeFolder];
        }
    },
    watch: {
        // dirty hack to load the first time
        folder(value, oldValue) {
            if (value && !oldValue) {
                this.refreshList();
            }
        }
    },
    methods: {
        ...mapActions("mail", { FETCH_MESSAGE_METADATA, REFRESH_CONVERSATION_LIST_KEYS }),
        async refreshList() {
            const conversationsActivated = this.settings.mail_thread === "true" && this.folder.allowConversations;
            await this.REFRESH_CONVERSATION_LIST_KEYS({ folder: this.folder, conversationsActivated });
            const messagesToFetch = this.CONVERSATION_LIST_KEYS.flatMap(key => this.CONVERSATION_MESSAGE_BY_KEY(key));
            this.FETCH_MESSAGE_METADATA({ messages: messagesToFetch, activeFolderKey: this.activeFolder });
        }
    },
    bus: {
        [PUSHED_FOLDER_CHANGES]: function (folderUid) {
            if (this.folders[this.activeFolder].remoteRef.uid === folderUid) {
                this.refreshList();
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";
.mail-conversation-list {
    outline: none;
    border-right: 1px solid $alternate-light;
}
</style>
