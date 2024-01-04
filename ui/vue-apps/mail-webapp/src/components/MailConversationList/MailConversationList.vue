<template>
    <section
        :aria-label="$t('mail.application.region.messagelist')"
        :class="{ 'search-typing': IS_TYPING_IN_SEARCH, 'mail-conversation-list': true }"
        :style="mailConversationListWidth"
    >
        <search-result-header v-if="IS_SEARCH_ENABLED" />
        <folder-result-header v-else />
        <search-input-mobile v-if="HAS_PATTERN" class="mobile-only" />
        <search-result v-if="IS_SEARCH_ENABLED" class="mail-conversation-list-content" />
        <folder-result v-else class="mail-conversation-list-content" />
    </section>
</template>

<script>
import { mapGetters, mapState, mapActions, mapMutations } from "vuex";
import debounce from "lodash.debounce";

import {
    CONVERSATION_LIST_COUNT,
    IS_SEARCH_ENABLED,
    CONVERSATION_LIST_KEYS,
    CONVERSATION_MESSAGE_BY_KEY,
    CONVERSATIONS_ACTIVATED,
    HAS_PATTERN,
    IS_TYPING_IN_SEARCH
} from "~/getters";
import { SET_CONVERSATION_LIST_STATUS } from "~/mutations";
import { FETCH_CONVERSATIONS, FETCH_MESSAGE_METADATA, REFRESH_CONVERSATION_LIST_KEYS } from "~/actions";
import { MAX_CHUNK_SIZE } from "~/store/api/apiMessages";
import { ConversationListStatus } from "~/store/conversationList";
import FolderResult from "./FolderResult";
import FolderResultHeader from "./FolderResultHeader";
import SearchInputMobile from "./SearchInputMobile";
import SearchResult from "./SearchResult";
import SearchResultHeader from "./SearchResultHeader";
import { PUSHED_FOLDER_CHANGES } from "../VueBusEventTypes";

export default {
    name: "MailConversationList",
    components: {
        FolderResult,
        FolderResultHeader,
        SearchInputMobile,
        SearchResult,
        SearchResultHeader
    },
    data() {
        return {
            hidden: false
        };
    },
    computed: {
        ...mapGetters("mail", {
            CONVERSATION_MESSAGE_BY_KEY,
            CONVERSATION_LIST_COUNT,
            IS_SEARCH_ENABLED,
            CONVERSATION_LIST_KEYS,
            HAS_PATTERN,
            IS_TYPING_IN_SEARCH
        }),
        ...mapState("mail", ["activeFolder", "folders"]),
        ...mapState("mail", {
            conversationByKey: state => state.conversations.conversationByKey,
            hasMoreResults: state => state.conversationList.search.hasMoreResults
        }),
        folder() {
            return this.folders[this.activeFolder];
        },
        mailConversationListWidth() {
            const width = this.$store.state.mail.conversationList.width;
            return width ? "width: " + width : "";
        },
        incompleteSearch() {
            return !!this.hasMoreResults && this.CONVERSATION_LIST_COUNT < MAX_CHUNK_SIZE;
        }
    },
    watch: {
        async incompleteSearch(isIncomplete) {
            if (isIncomplete) {
                if (this.CONVERSATION_LIST_COUNT === 0) {
                    this.SET_CONVERSATION_LIST_STATUS(ConversationListStatus.LOADING);
                }
                this.refreshSearchList();
            }
        }
    },
    methods: {
        ...mapActions("mail", { FETCH_CONVERSATIONS, FETCH_MESSAGE_METADATA, REFRESH_CONVERSATION_LIST_KEYS }),
        ...mapMutations("mail", { SET_CONVERSATION_LIST_STATUS }),
        refreshSearchList: debounce(async function () {
            await this.refreshList();
            this.SET_CONVERSATION_LIST_STATUS(ConversationListStatus.SUCCESS);
        }, 5000),
        async refreshList() {
            const conversationsActivated = this.$store.getters[`mail/${CONVERSATIONS_ACTIVATED}`];
            await this.REFRESH_CONVERSATION_LIST_KEYS({ folder: this.folder, conversationsActivated });

            const conversations = this.CONVERSATION_LIST_KEYS.map(key => this.conversationByKey[key]);
            await this.FETCH_CONVERSATIONS({ conversations, folder: this.folder, conversationsActivated });

            this.FETCH_MESSAGE_METADATA({ messages: conversations.flatMap(({ messages }) => messages) });
        },
        isCurrentMailbox(folderUid) {
            return this.folders[folderUid].mailboxRef.key === this.folder.mailboxRef.key;
        }
    },
    bus: {
        [PUSHED_FOLDER_CHANGES]: async function (folderUid) {
            if (
                !this.IS_SEARCH_ENABLED &&
                (this.folder.remoteRef.uid === folderUid || (this.CONVERSATIONS_ACTIVATED && this.isCurrentMailbox()))
            ) {
                this.refreshList(folderUid);
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";
@import "~@bluemind/ui-components/src/css/utils/responsiveness";

.mail-conversation-list {
    width: 100%;
    height: 100%;
    min-width: 100%;

    @include from-lg {
        min-width: 20%;
        max-width: 70%;
        width: 30%;
    }

    display: flex;
    flex-direction: column;

    outline: none;
    background-color: $surface;

    .mail-conversation-list-content {
        flex: 1 1 auto !important;
        background-color: $backdrop;
    }
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
