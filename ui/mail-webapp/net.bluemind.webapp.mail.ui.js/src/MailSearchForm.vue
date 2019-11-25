<template>
    <bm-form-input
        v-model="searchedPattern"
        :placeholder="$t('common.search')"
        type="search"
        icon="search"
        :aria-label="$t('common.search')"
        class="mail-search-form rounded-0"
        @keydown.enter="doSearch"
        @update="onChange"
        @reset="cancel"
    />
</template>

<script>
import { BmFormInput } from "@bluemind/styleguide";
import { mapMutations, mapState } from "vuex";

const MILLISECONDS_BEFORE_DISPLAY_SPINNER = 50;
const MILLISECONDS_BEFORE_TRIGGER_SEARCH = 500;

export default {
    name: "MailSearchForm",
    components: { BmFormInput },
    data() {
        return {
            searchedPattern: this.$route.params.pattern || "",
            idSetTimeoutLoading: null,
            idSetTimeoutSearch: null
        };
    },
    computed: {
        ...mapState("mail-webapp", ["currentFolderUid", "search"]),
        inputIsEmpty() {
            return this.searchedPattern === "";
        }
    },
    watch: {
        "search.pattern": function() {
            if (this.search.pattern === null) {
                this.searchedPattern = "";
            }
        }
    },
    methods: {
        ...mapMutations("mail-webapp", ["setSearchPattern", "setSearchLoading", "setSearchError"]),
        doSearch() {
            if (this.searchedPattern != "") {
                this.$router.push("/mail/search/" + this.searchedPattern + "/");
            }
        },
        cancel() {
            this.searchedPattern = "";
            this.$router.push("/mail/" + this.currentFolderUid + "/");
        },
        onChange() {
            if (this.searchedPattern === "") {
                this.cancel();
            } else {
                if (this.idSetTimeoutLoading !== null) {
                    clearTimeout(this.idSetTimeoutLoading);
                }
                if (this.idSetTimeoutSearch !== null) {
                    clearTimeout(this.idSetTimeoutSearch);
                }
                this.idSetTimeoutLoading = setTimeout(this.setSearchLoading(true), MILLISECONDS_BEFORE_DISPLAY_SPINNER);
                this.idSetTimeoutSearch = setTimeout(this.doSearch, MILLISECONDS_BEFORE_TRIGGER_SEARCH);
            }
        }
    }
};
</script>
