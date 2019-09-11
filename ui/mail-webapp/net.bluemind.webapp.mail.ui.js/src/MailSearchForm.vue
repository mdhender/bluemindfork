<template>
    <div class="mail-search-form d-flex align-items-center justify-content-center">
        <bm-form-input
            v-model="searchedPattern"
            :placeholder="$t('common.search')"
            type="search"
            :aria-label="$t('common.search')"
            class="rounded-0"
            @keydown.enter="search"
            @update="onChange"
        />
        <bm-icon v-if="searchedPattern === ''" icon="search" class="searchIcon" />
        <bm-button-close v-else class="px-0" @click="cancel" />
    </div>
</template>

<script>
import { BmButtonClose, BmFormInput, BmIcon } from "@bluemind/styleguide";
import { mapGetters, mapMutations } from "vuex";

const MILLISECONDS_BEFORE_DISPLAY_SPINNER = 50;
const MILLISECONDS_BEFORE_TRIGGER_SEARCH = 500;

export default {
    name: "MailSearchForm",
    components: {
        BmButtonClose,
        BmFormInput,
        BmIcon
    },
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

<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

.mail-search-form .searchIcon,
.mail-search-form .close {
    margin-left: -map-get($spacers, 4);
}

.mail-search-form input {
    padding-right: map-get($spacers, 4);
}

.mail-search-form .close {
    height: 12px;
    width: 12px;
    font-size: 26px;
}

.mail-search-form .close:hover {
    color: $primary;
}
</style>
