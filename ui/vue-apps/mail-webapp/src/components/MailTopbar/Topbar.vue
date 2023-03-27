<template>
    <div class="topbar">
        <topbar-desktop class="d-none d-lg-flex" />
        <topbar-actions-message-mobile v-if="hasMessageDisplayed || SEVERAL_CONVERSATIONS_SELECTED" class="d-lg-none" />
        <topbar-search-mobile v-else-if="searchModeMobile" class="d-lg-none" />
        <topbar-conversation-list-mobile
            v-else
            class="d-lg-none"
            @showFolders="$emit('showFolders')"
            @showSearch="showSearch = true"
        />
    </div>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import { ACTIVE_MESSAGE, SEVERAL_CONVERSATIONS_SELECTED } from "~/getters";
import TopbarActionsMessageMobile from "./Mobile/TopbarActionsMessageMobile";
import TopbarConversationListMobile from "./Mobile/TopbarConversationListMobile";
import TopbarDesktop from "./TopbarDesktop";
import TopbarSearchMobile from "./Mobile/TopbarSearchMobile";

export default {
    components: { TopbarDesktop, TopbarActionsMessageMobile, TopbarConversationListMobile, TopbarSearchMobile },
    computed: {
        ...mapState("mail", {
            currentConversation: ({ conversations }) => conversations.currentConversation,
            searchModeMobile: ({ conversationList }) => conversationList.search.searchModeMobile
        }),
        ...mapGetters("mail", { ACTIVE_MESSAGE, SEVERAL_CONVERSATIONS_SELECTED }),
        hasMessageDisplayed() {
            return Boolean(this.ACTIVE_MESSAGE || this.currentConversation);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";
@import "~@bluemind/ui-components/src/css/mixins/_responsiveness";

.topbar {
    background-color: $surface-hi1;
    min-height: base-px-to-rem(46);
    .topbar-desktop,
    .topbar-actions-message-mobile,
    .topbar-search-mobile,
    .topbar-conversation-list-mobile {
        flex: 1 1 auto;
    }
    @include until-lg {
        background-color: $fill-primary-bg;
        color: $fill-primary-fg;
        font-weight: $font-weight-bold;
    }
    @media print {
        display: none !important;
    }
}
</style>
