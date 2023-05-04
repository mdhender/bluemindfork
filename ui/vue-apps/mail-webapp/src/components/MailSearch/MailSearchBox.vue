<template>
    <div class="mail-search-box d-flex" role="search" :class="{ active }" @click.stop>
        <global-events v-if="focusIn" @keydown.escape="shrinkBox" @click="shrinkBox" />
        <div
            ref="search-box-lg"
            class="d-none d-lg-flex flex-fill"
            @keydown.enter="currentSearch.pattern ? search() : null"
        >
            <div class="d-flex flex-fill align-items-center box pl-1" :class="{ active }">
                <mail-search-box-context v-if="active" class="context" :folder="currentFolder" />
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
                    :disabled="isSameSearch"
                    @click="searchIfValid"
                    @keydown.enter.native="searchIfValid"
                >
                    {{ $t("common.action.search") }}
                </bm-button>
            </div>
        </div>
        <div class="d-lg-none">
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
import {
    RESET_CURRENT_SEARCH_PATTERN,
    SET_CURRENT_SEARCH_PATTERN,
    SET_CURRENT_SEARCH_DEEP,
    SET_CURRENT_SEARCH_FOLDER
} from "~/mutations";

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
            return this.folders[this.activeFolder];
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
        ...mapMutations("mail", {
            RESET_CURRENT_SEARCH_PATTERN,
            SET_CURRENT_SEARCH_PATTERN,
            SET_CURRENT_SEARCH_DEEP,
            SET_CURRENT_SEARCH_FOLDER
        }),
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
        shrinkBox(event) {
            const modal = document.getElementById("advanced-search-modal")?.getElementsByClassName("modal-content")[0];
            if (!event.path.some(element => modal === element)) {
                this.focusIn = false;
            }
            this.$refs.search.blur();
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
@import "~@bluemind/ui-components/src/css/mixins/_responsiveness";
@import "~@bluemind/ui-components/src/css/variables";

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
        background: $surface;
        margin-left: -$input-border-width * 2;
    }
    .mail-search-box-context > .dropdown-toggle {
        &,
        &:hover {
            border-color: transparent !important;
            border-right-color: $neutral-fg-lo3 !important;
        }
    }
    .mail-search-input > .bm-form-input > input {
        @include from-lg {
            border: none !important;
        }
    }
}

@include until-lg {
    .box {
        .form-control,
        .form-control::placeholder,
        .form-control:focus,
        .toggle-button,
        .toggle-button:focus,
        .toggle-button:active,
        .toggle-button::before,
        .close,
        .bm-icon {
            color: $fill-primary-fg !important;
            background: transparent !important;
        }
    }
}
</style>
