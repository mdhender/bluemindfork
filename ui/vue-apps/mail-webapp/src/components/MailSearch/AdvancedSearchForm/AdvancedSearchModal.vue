<template>
    <bm-modal
        id="advanced-search-modal"
        size="lg"
        :ok-title="$t('common.action.search')"
        hide-header
        :busy="!newPattern"
        :cancel-title="$t('common.action.reset')"
        @cancel="cancel"
        @hide="SET_CURRENT_SEARCH_PATTERN(newPattern)"
        @ok="validateAndsearch"
    >
        <div class="item">
            <div class="label">{{ $t("mail.search.options.folder.label") }}</div>
            <mail-search-box-context class="search-input" :folder="currentFolder" />
        </div>
        <h3 class="section">{{ $t("mail.search.section.msg_and_attachments") }}</h3>
        <div class="item">
            <div class="label">{{ $t("mail.search.label.subject") }}</div>
            <string-search-input class="search-input" :value.sync="subject" />
        </div>
        <div class="item">
            <div class="label">{{ $t("mail.search.label.contains") }}</div>
            <string-search-input class="search-input" :value.sync="contains" />
        </div>
        <h3 class="section">{{ $t("common.date") }}</h3>
        <div class="item d-flex align-items-center">
            <div class="label">{{ $t("mail.search.label.after") }}</div>
            <date-search-input class="search-input" :value.sync="after" />
        </div>
        <div class="item">
            <div class="label">{{ $t("mail.search.label.before") }}</div>
            <date-search-input class="search-input" :value.sync="before" />
        </div>
    </bm-modal>
</template>

<script>
import { mapActions, mapMutations, mapState } from "vuex";
import { BmModal } from "@bluemind/ui-components";
import { SearchMixin } from "~/mixins";
import { RESET_CURRENT_SEARCH } from "~/actions";
import {
    SET_CURRENT_SEARCH_DEEP,
    SET_CURRENT_SEARCH_FOLDER,
    SET_CURRENT_SEARCH_PATTERN,
    RESET_CURRENT_SEARCH_PATTERN
} from "~/mutations";
import MailSearchBoxContext from "../MailSearchBoxContext";
import SearchHelper from "../SearchHelper";
import StringSearchInput from "./StringSearchInput";
import DateSearchInput from "./DateSearchInput";
import PATTERN_KEYWORDS from "../SearchHelper/Keywords";

export default {
    name: "AdvancedSearchModal",
    components: { BmModal, MailSearchBoxContext, StringSearchInput, DateSearchInput },
    mixins: [SearchMixin],
    data() {
        return {
            contains: null,
            subject: null,
            date: null
        };
    },
    computed: {
        ...mapState("mail", ["folders", "activeFolder"]),
        ...mapState("mail", {
            currentPattern: ({ conversationList }) => conversationList.search.currentSearch.pattern
        }),
        currentFolder() {
            return this.folders[this.activeFolder];
        },
        newPattern() {
            const subParts = Object.values(PATTERN_KEYWORDS).flatMap(keyword => {
                return this[keyword] ? `${keyword}:${this.stringify(keyword)}` : [];
            });

            if (this.contains) {
                subParts.push(this.contains);
            }
            return subParts.join(" ");
        },
        before: {
            get() {
                return this.date?.max;
            },
            set(value) {
                this.date = { ...this.date, max: value };
            }
        },
        after: {
            get() {
                return this.date?.min;
            },
            set(value) {
                this.date = { ...this.date, min: value };
            }
        }
    },
    watch: {
        currentPattern: {
            handler() {
                const keywords = Object.values(PATTERN_KEYWORDS);
                const groups = SearchHelper.parseSearchPattern(this.currentPattern, keywords);
                for (const keyword of keywords) {
                    this[keyword] = groups[keyword];
                }
                this.contains = groups.contains;
            },
            immediate: true
        }
    },
    methods: {
        ...mapMutations("mail", {
            SET_CURRENT_SEARCH_DEEP,
            SET_CURRENT_SEARCH_FOLDER,
            SET_CURRENT_SEARCH_PATTERN,
            RESET_CURRENT_SEARCH_PATTERN
        }),
        ...mapActions("mail", { RESET_CURRENT_SEARCH }),
        updateDeep(value) {
            this.SET_CURRENT_SEARCH_DEEP(value);
        },
        updateFolder(value) {
            this.SET_CURRENT_SEARCH_FOLDER(value);
        },
        cancel(event) {
            event.preventDefault();
            this.contains = null;
            this.subject = null;
            this.date = null;
        },
        validateAndsearch() {
            this.SET_CURRENT_SEARCH_PATTERN(this.newPattern);
            this.search();
        },
        stringify(keyword) {
            switch (keyword) {
                case "date": {
                    return this.date?.min ? `[${this.date.min || "*"} TO ${this.date.max || "*"}]` : null;
                }
                default: {
                    return this[keyword];
                }
            }
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/ui-components/src/css/mixins/responsiveness";
@import "@bluemind/ui-components/src/css/variables";

#advanced-search-modal {
    .modal-content {
        overflow: visible;
    }
    .modal-body {
        overflow: visible;
        .section {
            margin-top: $sp-7;
            margin-bottom: $sp-6;
        }

        @include until-lg {
            padding: $sp-4;

            & > .item {
                display: block;
                width: 100%;
                margin-bottom: $sp-6;

                & > .input {
                    width: 100%;
                }
            }
        }
        @include from-lg {
            padding-top: $sp-6;

            & > .item {
                display: flex;
                align-items: center;
                flex: 1 1 auto;
                margin-bottom: $sp-4;
                min-height: $input-height;
                & > .label {
                    display: table-cell;
                    min-width: 25%;
                    text-align: end;
                }
                .search-input {
                    margin-left: $sp-6;
                }
                .date-search-input {
                    min-width: 16rem;
                }
                .mail-search-box-context {
                    width: 20rem;
                }
            }
        }
    }
}
</style>
