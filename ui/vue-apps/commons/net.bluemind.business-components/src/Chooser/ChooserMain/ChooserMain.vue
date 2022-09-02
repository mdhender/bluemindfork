<template>
    <search-result-files-table v-if="isActiveSearchMode" class="chooser-main" />
    <directory-files-table v-else class="chooser-main" />
</template>

<script>
import { mapGetters, mapState } from "vuex";
import { HAS_VALID_PATTERN } from "../store/getters";
import { RESET_SELECTED_FILES } from "../store/mutations";
import DirectoryFilesTable from "./DirectoryFilesTable";
import SearchResultFilesTable from "./SearchResultFilesTable";

export default {
    name: "ChooserMain",
    components: { DirectoryFilesTable, SearchResultFilesTable },
    computed: {
        ...mapState("chooser", ["isSearchMode"]),
        ...mapGetters("chooser", [HAS_VALID_PATTERN]),
        isActiveSearchMode() {
            return this.isSearchMode && HAS_VALID_PATTERN;
        }
    },
    watch: {
        isActiveSearchMode() {
            this.$store.commit(`chooser/${RESET_SELECTED_FILES}`);
        }
    }
};
</script>

<style></style>
