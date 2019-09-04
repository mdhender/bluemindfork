<template>
    <bm-form-input
        v-model="searchedPattern"
        :placeholder="$t('common.search')"
        type="search"
        icon="search"
        :aria-label="$t('common.search')"
        class="mail-search-form rounded-0"
        @keydown.enter="search"
        @update="onChange"
        @reset="cancel"
    />
</template>

<script>
import { BmFormInput } from "@bluemind/styleguide";
import { mapGetters, mapMutations } from "vuex";

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
        ...mapGetters("backend.mail/folders", ["currentFolder"]),
        inputIsEmpty() {
            return this.searchedPattern === "";
        }
    },
    watch: {
        currentFolder() {
            this.searchedPattern = "";
        }
    },
    methods: {
        ...mapMutations("backend.mail/items", ["setSearchPattern", "setSearchLoading", "setSearchError"]),
        search() {
            if (this.searchedPattern != "") {
                this.$router.push({ name: "search", params: { pattern: this.searchedPattern } });
            }
        },
        cancel() {
            this.searchedPattern = "";
            this.setSearchPattern(null);
            this.setSearchLoading(false);
            this.setSearchError(false);
            this.$router.push("/mail/" + this.currentFolder);
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
                this.idSetTimeoutSearch = setTimeout(this.search, MILLISECONDS_BEFORE_TRIGGER_SEARCH);
            }
        }
    }
};
</script>
