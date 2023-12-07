<template>
    <div class="topbar">
        <topbar-desktop class="desktop-only shadow" />
        <topbar-actions-message-mobile
            v-if="hasMessageDisplayed || SEVERAL_CONVERSATIONS_SELECTED"
            class="mobile-only"
        />
        <topbar-search-mobile v-else-if="HAS_PATTERN" class="mobile-only" />
        <topbar-recoverable-list-mobile v-else-if="CONVERSATION_LIST_DELETED_FILTER_ENABLED" class="mobile-only" />
        <topbar-conversation-list-mobile v-else class="mobile-only" @showFolders="$emit('showFolders')" />
        <advanced-search-modal />
    </div>
</template>

<script>
import { mapGetters } from "vuex";
import {
    ACTIVE_MESSAGE,
    HAS_PATTERN,
    CONVERSATION_LIST_DELETED_FILTER_ENABLED,
    SEVERAL_CONVERSATIONS_SELECTED
} from "~/getters";
import TopbarActionsMessageMobile from "./Mobile/TopbarActionsMessageMobile";
import TopbarConversationListMobile from "./Mobile/TopbarConversationListMobile";
import TopbarRecoverableListMobile from "./Mobile/TopbarRecoverableListMobile";
import TopbarDesktop from "./TopbarDesktop";
import TopbarSearchMobile from "./Mobile/TopbarSearchMobile";
import AdvancedSearchModal from "../MailSearch/AdvancedSearchForm/AdvancedSearchModal";

export default {
    components: {
        AdvancedSearchModal,
        TopbarActionsMessageMobile,
        TopbarConversationListMobile,
        TopbarRecoverableListMobile,
        TopbarDesktop,
        TopbarSearchMobile
    },
    computed: {
        ...mapGetters("mail", {
            ACTIVE_MESSAGE,
            HAS_PATTERN,
            CONVERSATION_LIST_DELETED_FILTER_ENABLED,
            SEVERAL_CONVERSATIONS_SELECTED
        }),
        hasMessageDisplayed() {
            return Boolean(this.ACTIVE_MESSAGE || this.currentConversation);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";
@import "~@bluemind/ui-components/src/css/utils/responsiveness";

.topbar {
    .navbar {
        padding-right: $sp-3;
        min-width: 0;
        flex: 1;
    }
    @media print {
        display: none !important;
    }
}
</style>
