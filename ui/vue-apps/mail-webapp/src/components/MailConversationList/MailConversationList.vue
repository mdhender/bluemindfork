<template>
    <div class="px-0 d-lg-block mail-conversation-list-wrapper" :style="mailConversationListWidth">
        <section
            :aria-label="$t('mail.application.region.messagelist')"
            class="mail-conversation-list d-flex flex-column h-100"
            :class="{ 'search-typing': IS_TYPING_IN_SEARCH }"
        >
            <mail-conversation-list-header id="mail-conversation-list-header" />
            <search-input-mobile v-if="HAS_PATTERN" class="d-lg-none" />
            <search-result v-if="CONVERSATION_LIST_IS_FILTERED" class="flex-fill" />
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
    CONVERSATION_LIST_IS_FILTERED,
    CONVERSATION_LIST_KEYS,
    HAS_PATTERN,
    IS_TYPING_IN_SEARCH
} from "~/getters";
import { FETCH_CONVERSATIONS, FETCH_MESSAGE_METADATA, REFRESH_CONVERSATION_LIST_KEYS } from "~/actions";
import { PUSHED_FOLDER_CHANGES } from "../VueBusEventTypes";
import SearchInputMobile from "./SearchInputMobile";

export default {
    name: "MailConversationList",
    components: {
        MailConversationListHeader,
        SearchResult,
        FolderResult,
        SearchInputMobile
    },
    data() {
        return {
            hidden: false
        };
    },
    computed: {
        ...mapGetters("mail", {
            CONVERSATION_MESSAGE_BY_KEY,
            CONVERSATION_LIST_IS_FILTERED,
            CONVERSATION_LIST_KEYS,
            HAS_PATTERN,
            IS_TYPING_IN_SEARCH
        }),
        ...mapState("mail", ["activeFolder", "folders"]),
        ...mapState("mail", {
            conversationByKey: state => state.conversations.conversationByKey
        }),
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
            if (!this.CONVERSATION_LIST_IS_FILTERED && this.folders[this.activeFolder].remoteRef.uid === folderUid) {
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
@import "~@bluemind/ui-components/src/css/mixins/_responsiveness";

.mail-conversation-list {
    background-color: $surface;
    outline: none;
    border-right: 1px solid $neutral-fg-lo2;
    @include until-lg {
        &.search-typing {
            background-color: $neutral-bg;
            .search-result,
            .folder-result {
                display: none;
            }
        }
    }
}
</style>
