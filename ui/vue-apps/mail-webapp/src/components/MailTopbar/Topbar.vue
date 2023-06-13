<template>
    <div class="topbar">
        <topbar-desktop class="d-none d-lg-flex" />
        <topbar-actions-message-mobile v-if="hasMessageDisplayed || SEVERAL_CONVERSATIONS_SELECTED" class="d-lg-none" />
        <topbar-search-mobile v-else-if="HAS_PATTERN" class="d-lg-none" />
        <topbar-conversation-list-mobile v-else class="d-lg-none" @showFolders="$emit('showFolders')" />
        <advanced-search-modal />
    </div>
</template>

<script>
import { mapGetters } from "vuex";
import { ACTIVE_MESSAGE, HAS_PATTERN, SEVERAL_CONVERSATIONS_SELECTED } from "~/getters";
import TopbarActionsMessageMobile from "./Mobile/TopbarActionsMessageMobile";
import TopbarConversationListMobile from "./Mobile/TopbarConversationListMobile";
import TopbarDesktop from "./TopbarDesktop";
import TopbarSearchMobile from "./Mobile/TopbarSearchMobile";
import AdvancedSearchModal from "../MailSearch/AdvancedSearchForm/AdvancedSearchModal";

export default {
    components: {
        AdvancedSearchModal,
        TopbarActionsMessageMobile,
        TopbarConversationListMobile,
        TopbarDesktop,
        TopbarSearchMobile
    },
    computed: {
        ...mapGetters("mail", { ACTIVE_MESSAGE, HAS_PATTERN, SEVERAL_CONVERSATIONS_SELECTED }),
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
    background-color: $surface-hi1;
    min-height: base-px-to-rem(48);
    .topbar-desktop,
    .topbar-actions-message-mobile,
    .topbar-search-mobile,
    .topbar-conversation-list-mobile {
        flex: 1 1 auto;
    }
    @include until-lg {
        background-color: $fill-primary-bg;
        color: $fill-primary-fg;
    }
    @media print {
        display: none !important;
    }
}
</style>
