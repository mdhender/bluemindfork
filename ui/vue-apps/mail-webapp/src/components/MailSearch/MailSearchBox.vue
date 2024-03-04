<template>
    <div class="mail-search-box d-flex" role="search" :class="{ active }">
        <global-events v-if="focusIn" @click="shrinkBox" @keydown.enter="shrinkBox" @keydown.escape="blur" />
        <div
            ref="search-box-lg"
            class="desktop-only d-flex flex-fill mr-5"
            @keydown.enter="currentSearch.pattern ? search() : null"
        >
            <div class="d-flex flex-fill align-items-center box" :class="{ active }">
                <mail-search-box-context
                    v-if="active"
                    class="context"
                    :folder="currentFolder"
                    @update="searchIfValid"
                />
                <mail-search-input
                    ref="search"
                    resettable
                    :size="active ? 'md' : 'sm'"
                    @reset="reset"
                    @active="focusIn = true"
                />
                <mail-search-advanced-button v-show="active" class="mx-3" />
            </div>
            <div class="search-button-wrapper">
                <bm-button
                    size="lg"
                    class="mail-search-button"
                    variant="fill-accent"
                    :disabled="!currentSearch.pattern || isSameSearch"
                    @click="searchIfValid"
                    @keydown.enter.native="searchIfValid"
                >
                    {{ $t("common.action.search") }}
                </bm-button>
            </div>
        </div>
        <div class="mobile-only">
            <bm-icon-button
                size="lg"
                variant="compact-on-fill-primary"
                icon="search"
                @click="SET_CURRENT_SEARCH_PATTERN('')"
            />
        </div>
    </div>
</template>

<script>
import { mapMutations, mapState } from "vuex";
import GlobalEvents from "vue-global-events";
import { BmIconButton, BmButton } from "@bluemind/ui-components";
import { SearchMixin } from "~/mixins";
import { RESET_CURRENT_SEARCH_PATTERN, SET_CURRENT_SEARCH_PATTERN } from "~/mutations";

import MailSearchAdvancedButton from "./MailSearchAdvancedButton";
import MailSearchBoxContext from "./MailSearchBoxContext";
import MailSearchInput from "./MailSearchInput";
import SearchHelper from "./SearchHelper";

export default {
    name: "MailSearchBox",
    components: {
        BmButton,
        BmIconButton,
        GlobalEvents,
        MailSearchAdvancedButton,
        MailSearchBoxContext,
        MailSearchInput
    },
    mixins: [SearchMixin],
    data() {
        return {
            focusIn: false
        };
    },
    computed: {
        ...mapState("mail", ["folders", "mailboxes", "activeFolder"]),
        ...mapState("mail", {
            searchQuery: ({ conversationList }) => conversationList.search.searchQuery,
            currentSearch: ({ conversationList }) => conversationList.search.currentSearch
        }),
        currentFolder() {
            return this.folders[this.activeFolder] || {};
        },
        active() {
            return !!this.currentSearch.pattern || this.focusIn;
        },
        isSameSearch() {
            return SearchHelper.isSameSearch(
                this.searchQuery.pattern,
                this.currentSearch.pattern,
                this.searchQuery.folder?.key,
                this.currentSearch.folder?.key,
                this.searchQuery.deep,
                this.currentSearch.deep
            );
        }
    },
    watch: {
        active: {
            handler(isActive) {
                this.$emit("active", isActive);
            }
        }
    },
    methods: {
        ...mapMutations("mail", { RESET_CURRENT_SEARCH_PATTERN, SET_CURRENT_SEARCH_PATTERN }),
        reset() {
            this.cancel();
            this.focusIn = false;
            this.RESET_CURRENT_SEARCH_PATTERN();
            this.$router.navigate({ name: "v:mail:home", params: { search: null } });
        },
        cancel() {
            this.cancelRoute();
            this.cancelSpinner();
        },
        cancelRoute() {
            this.$router.navigate({
                name: "v:mail:home"
            });
        },
        blur() {
            this.focusIn = false;
            this.$refs.search.blur();
        },
        shrinkBox(event) {
            const modal = document.getElementById("advanced-search-modal")?.getElementsByClassName("modal-content")[0];
            const path = event.path || event.composedPath();
            if (!path.some(element => modal === element || this.$el === element)) {
                this.blur();
            }
        },
        searchIfValid() {
            if (this.currentSearch.pattern) {
                this.search();
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";

.mail-search-box {
    &:not(.active) {
        .search-button-wrapper {
            display: none;
        }
        .mail-search-box-context {
            display: none;
        }
    }
    .box {
        outline: $input-border-width solid $neutral-fg;
        outline-offset: -$input-border-width;

        &:focus-within {
            outline: 2 * $input-border-width solid $secondary-fg;
            outline-offset: -2 * $input-border-width;
        }
    }
    .search-button-wrapper {
        background: $surface-hi1;
        margin-left: -$input-border-width * 2;
    }
    .mail-search-box-context > .dropdown-toggle {
        &,
        &:hover {
            border-color: transparent !important;
            border-right-color: $neutral-fg-lo3 !important;
        }
    }
    .mail-search-input > input {
        &,
        &:hover {
            @include from-lg {
                border: none !important;
            }
        }
    }
}
</style>
