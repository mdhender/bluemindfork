<template>
    <div class="px-0 d-lg-block mail-conversation-list-wrapper" :style="mailConversationListWidth">
        <section
            :aria-label="$t('mail.application.region.messagelist')"
            class="mail-conversation-list d-flex flex-column h-100"
        >
            <mail-conversation-list-header id="mail-conversation-list-header" />
            <search-result v-if="CONVERSATION_LIST_IS_SEARCH_MODE" class="flex-fill" />
            <folder-result v-else class="flex-fill" />
        </section>
    </div>
</template>

<script>
import { mapGetters, mapState, mapActions } from "vuex";
import MailConversationListHeader from "./MailConversationListHeader";
import SearchResult from "./SearchResult";
import FolderResult from "./FolderResult";
import {
    CONVERSATIONS_ACTIVATED,
    CONVERSATION_MESSAGE_BY_KEY,
    CONVERSATION_LIST_IS_SEARCH_MODE,
    CONVERSATION_LIST_KEYS
} from "~/getters";
import { FETCH_CONVERSATIONS, FETCH_MESSAGE_METADATA, REFRESH_CONVERSATION_LIST_KEYS } from "~/actions";
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
        ...mapState("mail", ["activeFolder", "folders"]),
        ...mapState("mail", { conversationByKey: state => state.conversations.conversationByKey }),
        folder() {
            return this.folders[this.activeFolder];
        },
        mailConversationListWidth() {
            const width = this.$store.state.mail.conversationList.width;
            return width ? "width: " + width : "";
        }
    },
    methods: {
        ...mapActions("mail", { FETCH_CONVERSATIONS, FETCH_MESSAGE_METADATA, REFRESH_CONVERSATION_LIST_KEYS })
    },
    bus: {
        [PUSHED_FOLDER_CHANGES]: async function (folderUid) {
            if (!this.CONVERSATION_LIST_IS_SEARCH_MODE && this.folders[this.activeFolder].remoteRef.uid === folderUid) {
                const conversationsActivated = this.$store.getters[`mail/${CONVERSATIONS_ACTIVATED}`];
                await this.REFRESH_CONVERSATION_LIST_KEYS({ folder: this.folder, conversationsActivated });

                const conversations = this.CONVERSATION_LIST_KEYS.map(key => this.conversationByKey[key]);
                await this.FETCH_CONVERSATIONS({ conversations, folder: this.folder, conversationsActivated });

                this.FETCH_MESSAGE_METADATA({ messages: conversations.flatMap(({ messages }) => messages) });
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";
.mail-conversation-list {
    outline: none;
    border-right: 1px solid $neutral-fg-lo1;
}
</style>
