<template>
    <div class="search-input-mobile">
        <mail-search-input ref="search" @keydown.enter.native="onSearch" @focus="$emit('focus')" />
    </div>
</template>

<script>
import { mapMutations, mapState } from "vuex";
import { SearchMixin } from "~/mixins";
import { SET_CURRENT_SEARCH_FOLDER, SET_CURRENT_SEARCH_DEEP } from "~/mutations";
import { folderUtils } from "@bluemind/mail";
import MailSearchInput from "../MailSearch/MailSearchInput";

const { DEFAULT_FOLDERS } = folderUtils;

export default {
    name: "SearchInputMobile",
    components: { MailSearchInput },
    mixins: [SearchMixin],
    computed: {
        ...mapState("mail", ["folders", "activeFolder"]),
        ...mapState("mail", {
            currentSearch: ({ conversationList }) => conversationList.search.currentSearch
        }),
        currentFolder() {
            return this.folders[this.activeFolder];
        }
    },
    mounted() {
        this.$refs.search.focus();
    },
    methods: {
        ...mapMutations("mail", { SET_CURRENT_SEARCH_FOLDER, SET_CURRENT_SEARCH_DEEP }),
        onSearch() {
            const folder = this.currentFolder.imapName === DEFAULT_FOLDERS.INBOX ? { key: null } : this.currentFolder;
            this.SET_CURRENT_SEARCH_FOLDER(folder);
            this.SET_CURRENT_SEARCH_DEEP(true);
            this.search();
            this.$refs.search.blur();
        }
    }
};
</script>
<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

.search-input-mobile {
    & > .mail-search-input {
        background: $surface;
        & > input {
            border-color: $neutral-fg-lo3;
        }
    }
}
</style>
