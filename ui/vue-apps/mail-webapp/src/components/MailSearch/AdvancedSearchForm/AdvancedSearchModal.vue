<template>
    <bm-modal
        id="advanced-search-modal"
        variant="advanced"
        size="lg"
        height="lg"
        scrollable
        :title="$t('mail.search.options.modal.title')"
        :ok-title="$t('common.action.search')"
        :busy="!newPattern"
        :cancel-title="$t('common.action.reset')"
        @cancel="cancel"
        @hide="sync"
        @ok="validateAndsearch"
    >
        <div class="item">
            <label>{{ $t("mail.search.options.folder.label") }}</label>
            <mail-search-box-context class="search-input" :folder="currentFolder" />
        </div>

        <h3 class="section">{{ $t("mail.search.section.contacts") }}</h3>
        <div class="item">
            <label>{{ $t("common.from") }}</label>
            <contact-search-input class="search-input" :addresses.sync="from" :max-contacts="1" />
        </div>
        <div class="item">
            <label>{{ $t("common.to") }}</label>
            <contact-search-input class="search-input" :addresses.sync="to" />
        </div>
        <div class="item">
            <label>{{ $t("common.cc") }}</label>
            <contact-search-input class="search-input" :addresses.sync="cc" />
        </div>
        <div class="item">
            <label>{{ $t("common.with") }}</label>
            <contact-search-input class="search-input" :addresses.sync="with_" />
        </div>
        <h3 class="section">{{ $t("mail.search.section.msg_and_attachments") }}</h3>
        <div class="item">
            <label>{{ $t("mail.search.label.subject") }}</label>
            <string-search-input class="search-input" :value.sync="subject" />
        </div>
        <div class="item">
            <label>{{ $t("mail.search.label.contains") }}</label>
            <string-search-input class="search-input" :value.sync="contains" />
        </div>
        <div class="item">
            <label>{{ $t("common.size") }}</label>
            <size-search-input :min.sync="sizeMin" :max.sync="sizeMax" class="search-input" />
        </div>
        <div class="item checkbox-item">
            <label :for="uniqueAttachmentId" class="desktop-only">{{ $t("common.attachment") }}</label>
            <bm-form-checkbox :id="uniqueAttachmentId" v-model="hasAttachment" class="search-input desktop-only" />
            <bm-form-checkbox v-model="hasAttachment" class="search-input mobile-only">
                {{ $t("common.attachment") }}
            </bm-form-checkbox>
        </div>
        <div class="item">
            <label>{{ $t("mail.search.label.filename") }}</label>
            <string-search-input class="search-input" :value.sync="filename" />
        </div>
        <h3 class="section">{{ $t("common.date") }}</h3>
        <div class="item">
            <label>{{ $t("common.from_date") }}</label>
            <date-search-input class="search-input" :value.sync="startingFrom" :max="until" />
        </div>
        <div class="item">
            <label>{{ $t("common.until") }}</label>
            <date-search-input class="search-input" :value.sync="until" :min="startingFrom" />
        </div>
    </bm-modal>
</template>

<script>
import { mapMutations, mapState } from "vuex";
import { BmModal, BmFormCheckbox } from "@bluemind/ui-components";
import { SearchMixin } from "~/mixins";
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
import ContactSearchInput from "./ContactSearchInput";

function initData() {
    return {
        contains: null,
        subject: null,
        date: null,
        filename: null,
        has: null,
        size: null,
        cc: null,
        to: null,
        with: null,
        from: null
    };
}
export default {
    name: "AdvancedSearchModal",
    components: {
        BmFormCheckbox,
        BmModal,
        ContactSearchInput,
        DateSearchInput,
        MailSearchBoxContext,
        SizeSearchInput,
        StringSearchInput
    },
    mixins: [SearchMixin],
    data() {
        return initData();
    },
    computed: {
        ...mapState("mail", ["folders", "activeFolder"]),
        ...mapState("mail", {
            currentPattern: ({ conversationList }) => conversationList.search.currentSearch.pattern
        }),
        currentFolder() {
            return this.folders[this.activeFolder] || {};
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
        until: {
            get() {
                return this.date?.max;
            },
            set(value) {
                this.date = { ...this.date, max: value };
            }
        },
        startingFrom: {
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
        },
        with_: {
            get() {
                return this.with;
            },
            set(value) {
                this.with = value;
            }
        },
        uniqueAttachmentId() {
            return `${this._uid}-attachment`;
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
                    return this.date?.min || this.date?.max
                        ? `[${this.date.min || "*"} TO ${this.date.max || "*"}]`
                        : null;
                }
                case "has": {
                    return this.hasAttachment ? "attachments" : null;
                }
                case "size": {
                    return +this.sizeMin > 0 ? `>${this.sizeMin}` : +this.sizeMax > 0 ? `<${this.sizeMax}` : null;
                }
                case "to":
                case "from":
                case "cc":
                case "with": {
                    return !this[keyword]?.length
                        ? null
                        : Array.isArray(this[keyword]) && this[keyword].length > 1
                        ? `(${this[keyword].join(" AND ")})`
                        : this[keyword];
                }
                default: {
                    const isMultiWord = /\s+/.test(this[keyword]?.trim());
                    return isMultiWord ? `(${this[keyword]})` : this[keyword];
                }
            }
        },
        resetFields() {
            const data = initData();
            Object.keys(data).forEach(property => (this[property] = data[property]));
        },
        sync(event) {
            if (event.trigger === "cancel") {
                event.preventDefault();
            } else {
                this.SET_CURRENT_SEARCH_PATTERN(this.newPattern);
                if (!this.newPattern) {
                    this.$router.navigate({ name: "v:mail:home", params: { search: null } });
                }
            }
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/ui-components/src/css/utils/responsiveness";
@import "@bluemind/ui-components/src/css/utils/variables";

#advanced-search-modal {
    .modal-body {
        .section {
            margin-top: $sp-7;
            margin-bottom: $sp-6;
        }
        label {
            margin-bottom: 0;
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
                    label {
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
                & > label {
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
