<template>
    <div class="chooser-search">
        <back-button v-show="HAS_VALID_PATTERN" class="mr-4" @back="reset" />
        <bm-form-input
            v-model="pattern"
            :placeholder="$t('common.search')"
            icon="search"
            resettable
            left-icon
            :aria-label="$t('common.search')"
            autocomplete="off"
            type="text"
            @reset="reset"
            @keydown.enter="activateSearchMode"
            @focus="setEmptyPattern"
        />
        <search-button v-show="HAS_VALID_PATTERN" :disabled="!pattern" @search="activateSearchMode" />
    </div>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import { BmFormInput } from "@bluemind/styleguide";
import { SET_SEARCH_PATTERN, SET_SEARCH_MODE } from "../../store/mutations";
import { HAS_VALID_PATTERN } from "../../store/getters";
import BackButton from "./BackButton";
import SearchButton from "./SearchButton";

export default {
    name: "ChooserSearch",
    components: { BmFormInput, BackButton, SearchButton },
    data() {
        return { pattern: "" };
    },
    computed: {
        ...mapState("chooser", ["isSearchMode"]),
        ...mapGetters("chooser", [HAS_VALID_PATTERN])
    },
    watch: {
        pattern(newVal, oldVal) {
            if (oldVal === "" && newVal) {
                this.$store.commit(`chooser/${SET_SEARCH_PATTERN}`, "");
            }
        }
    },
    methods: {
        activateSearchMode() {
            if (this.pattern) {
                this.$store.commit(`chooser/${SET_SEARCH_PATTERN}`, this.pattern);
                this.$store.commit(`chooser/${SET_SEARCH_MODE}`);
            }
        },
        reset() {
            this.pattern = "";
        },
        setEmptyPattern() {
            if (!this.HAS_VALID_PATTERN) {
                this.$store.commit(`chooser/${SET_SEARCH_PATTERN}`, "");
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.chooser-search {
    width: 100%;
    height: 100%;
    display: flex;
    flex: 1 1 auto;

    .bm-form-input {
        width: 100%;
    }
}
</style>
