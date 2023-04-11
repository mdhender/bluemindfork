<template>
    <div class="mail-search-form d-flex" role="search" :class="{ active: searchMode }" @click.stop>
        <global-events @click="hideForm" @keydown.escape="hideForm" />
        <div class="d-none d-lg-flex flex-fill">
            <div class="d-flex flex-fill mail-search-form-quicksearch" :class="{ active: searchMode }">
                <mail-search-form-context v-if="searchMode" class="context" @changed="context = $event" />
                <mail-search-field
                    ref="search"
                    :value.sync="pattern"
                    :size="searchMode ? 'md' : 'sm'"
                    @reset="reset"
                    @search="searchIfValid"
                    @active="SET_SEARCH_MODE(true)"
                />
            </div>
            <div class="search-button-wrapper">
                <bm-button
                    size="lg"
                    class="search-button"
                    variant="fill-accent"
                    :disabled="!pattern || pattern.length === 0 || isSameSearch"
                    @click="searchIfValid"
                >
                    {{ $t("common.action.search") }}
                </bm-button>
            </div>
        </div>
        <div class="d-lg-none">
            <bm-icon-button size="lg" variant="compact-on-fill-primary" icon="search" @click="SET_SEARCH_MODE(true)" />
        </div>
    </div>
</template>

<script>
import { mapMutations, mapState } from "vuex";
import GlobalEvents from "vue-global-events";
import { BmButton, BmIconButton, BmToggle } from "@bluemind/ui-components";
import { SearchMixin } from "~/mixins";
import { SET_SEARCH_MODE } from "~/mutations";

import { SearchHelper } from "./../SearchHelper";
import MailSearchField from "./MailSearchField";
import MailSearchFormContext from "./MailSearchFormContext";

export default {
    name: "MailSearchForm",
    components: {
        BmButton,
        BmIconButton,
        GlobalEvents,
        MailSearchField,
        MailSearchFormContext
    },
    directives: {
        BmToggle
    },
    mixins: [SearchMixin],
    data() {
        return {
            context: {}
        };
    },
    computed: {
        ...mapState("mail", {
            currentSearch: ({ conversationList }) => conversationList.search,
            searchMode: ({ conversationList }) => conversationList.search.searchMode
        }),
        ...mapState("mail", ["folders", "mailboxes", "activeFolder"]),
        isSameSearch() {
            return SearchHelper.isSameSearch(
                this.currentSearch.pattern,
                this.pattern,
                this.currentSearch.folder?.key,
                this.context.folder?.key,
                this.currentSearch.deep,
                this.context.deep
            );
        }
    },
    watch: {
        active(val) {
            this.SET_SEARCH_MODE(val);
        },
        context() {
            this.searchIfValid();
        }
    },
    methods: {
        ...mapMutations("mail", { SET_SEARCH_MODE }),
        reset() {
            this.cancel();
            this.pattern = null;
            this.context = {};
            this.SET_SEARCH_MODE(false);
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
        hideForm() {
            this.$refs.search.blur();
            if (!this.pattern) {
                this.SET_SEARCH_MODE(false);
            }
        },
        searchIfValid() {
            if (this.pattern) {
                this.search(this.pattern, { key: this.context.folder?.key }, this.context.deep);
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/mixins/_responsiveness";
@import "~@bluemind/ui-components/src/css/variables";

.mail-search-form {
    &:not(.active) {
        .search-button-wrapper {
            display: none;
        }
        .mail-search-form-context {
            display: none;
        }
    }
    .mail-search-form-quicksearch {
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
}

@include until-lg {
    .mail-search-form-quicksearch {
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
