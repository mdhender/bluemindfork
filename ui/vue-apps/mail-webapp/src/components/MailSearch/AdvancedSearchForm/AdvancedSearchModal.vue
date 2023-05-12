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
        <div class="item">
            <div class="label">{{ $t("common.size") }}</div>
            <size-search-input :min.sync="sizeMin" :max.sync="sizeMax" class="search-input" />
        </div>
        <div class="item checkbox-item">
            <div class="label">{{ $t("common.attachment") }}</div>
            <bm-form-checkbox v-model="hasAttachment" class="search-input" />
        </div>
        <div class="item">
            <div class="label">{{ $t("mail.search.label.filename") }}</div>
            <string-search-input class="search-input" :value.sync="filename" />
        </div>
        <h3 class="section">{{ $t("common.date") }}</h3>
        <div class="item">
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
import { BmModal, BmFormCheckbox } from "@bluemind/ui-components";
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
import parser from "../SearchHelper/patternParsers";
import SizeSearchInput from "./SizeSearchInput";

export default {
    name: "AdvancedSearchModal",
    components: { BmFormCheckbox, DateSearchInput, BmModal, MailSearchBoxContext, StringSearchInput, SizeSearchInput },
    mixins: [SearchMixin],
    data() {
        return {
            contains: null,
            subject: null,
            date: null,
            filename: null,
            has: null,
            size: null
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
            const subParts = Object.values(SearchHelper.PATTERN_KEYWORDS).flatMap(keyword => {
                return this.stringify(keyword) && keyword !== SearchHelper.PATTERN_KEYWORDS.CONTENT
                    ? `${keyword}:${this.stringify(keyword)}`
                    : [];
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
        },
        hasAttachment: {
            get() {
                return this.has?.includes("attachments");
            },
            set(value) {
                this.has = value ? "attachments" : null;
            }
        },
        sizeMin: {
            get() {
                return this.size?.min;
            },
            set(value) {
                this.size = { ...this.size, min: value };
            }
        },
        sizeMax: {
            get() {
                return this.size?.max;
            },
            set(value) {
                this.size = { ...this.size, max: value };
            }
        }
    },
    watch: {
        currentPattern: {
            handler() {
                this.resetFields();
                const groups = SearchHelper.parseSearchPattern(this.currentPattern);
                for (const keyword in groups) {
                    const value = groups[keyword];
                    this[keyword] = value && parser(value, keyword);
                }
                this.contains = groups.content;
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
            this.resetFields();
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
                case "has": {
                    return this.hasAttachment ? "attachments" : null;
                }
                case "size": {
                    return +this.sizeMin > 0 ? `>${this.sizeMin}` : +this.sizeMax > 0 ? `<${this.sizeMax}` : null;
                }
                default: {
                    return this[keyword];
                }
            }
        },
        resetFields() {
            this.contains = null;
            this.subject = null;
            this.date = null;
            this.has = null;
            this.filename = null;
            this.size = null;
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/ui-components/src/css/mixins/responsiveness";
@import "@bluemind/ui-components/src/css/variables";

#advanced-search-modal {
    .modal-body {
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

                & > .mail-search-box-context,
                & > .string-search-input,
                & > .size-search-input {
                    width: 100%;
                }
                &.checkbox-item {
                    display: flex;
                    align-items: center;
                    .search-input {
                        order: 1;
                    }
                    .label {
                        order: 2;
                    }
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
