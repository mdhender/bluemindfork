<template>
    <div class="mail-message-list d-flex flex-column">
        <mail-message-list-header id="mail-message-list-header" />
        <search-result v-if="isSearchMode" class="flex-fill" />
        <folder-result v-if="isFolderMode" class="flex-fill" />
    </div>
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
        ...mapGetters("mail-webapp", ["isSearchMode", "isFolderMode"])
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
