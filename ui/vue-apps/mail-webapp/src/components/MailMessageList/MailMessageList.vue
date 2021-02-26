<template>
    <section :aria-label="$t('mail.application.region.messagelist')" class="mail-message-list d-flex flex-column">
        <mail-message-list-header id="mail-message-list-header" />
        <search-result v-if="MESSAGE_LIST_IS_SEARCH_MODE" class="flex-fill" />
        <folder-result v-else class="flex-fill" />
    </section>
</template>

<script>
import { mapGetters, mapState, mapActions } from "vuex";
import MailMessageListHeader from "./MailMessageListHeader";
import SearchResult from "./SearchResult";
import FolderResult from "./FolderResult";
import { MESSAGE_LIST_IS_SEARCH_MODE } from "~getters";
import { REFRESH_MESSAGE_LIST_KEYS, FETCH_MESSAGE_METADATA } from "~actions";
import { PUSHED_FOLDER_CHANGES } from "../VueBusEventTypes";

export default {
    name: "MailMessageList",
    components: {
        MailMessageListHeader,
        SearchResult,
        FolderResult
    },
    computed: {
        ...mapGetters("mail", { MESSAGE_LIST_IS_SEARCH_MODE }),
        ...mapState("mail", ["activeFolder", "folders", "messages", "messageList"]),
        ...mapState("session", { settings: ({ settings }) => settings.remote })
    },
    methods: {
        ...mapActions("mail", { REFRESH_MESSAGE_LIST_KEYS, FETCH_MESSAGE_METADATA }),
        async refreshMessageList() {
            const folder = this.folders[this.activeFolder];
            const conversationsEnabled = this.userSettings.mail_thread === "true";
            await this.REFRESH_MESSAGE_LIST_KEYS({ folder, conversationsEnabled });
            const sorted = this.messageList.messageKeys.slice(0, 40).map(key => this.messages[key]);
            await this.FETCH_MESSAGE_METADATA(sorted);
        }
    },
    bus: {
        [PUSHED_FOLDER_CHANGES]: function (data) {
            if (this.activeFolder === data.body.mailbox) {
                this.refreshMessageList();
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";
.mail-message-list {
    outline: none;
    border-right: 1px solid $alternate-light;
}
</style>
