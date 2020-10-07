<template>
    <section :aria-label="$t('mail.application.region.messagelist')" class="mail-message-list d-flex flex-column">
        <mail-message-list-header id="mail-message-list-header" />
        <search-result v-if="MESSAGE_LIST_IS_SEARCH_MODE" class="flex-fill" />
        <folder-result v-else class="flex-fill" />
    </section>
</template>

<script>
import { mapGetters } from "vuex";
import MailMessageListHeader from "./MailMessageListHeader";
import SearchResult from "./SearchResult";
import FolderResult from "./FolderResult";

export default {
    name: "MailMessageList",
    components: {
        MailMessageListHeader,
        SearchResult,
        FolderResult
    },
    computed: {
        ...mapGetters("mail", ["MESSAGE_LIST_IS_SEARCH_MODE"])
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
