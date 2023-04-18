<template>
    <bm-modal
        id="advanced-search-modal"
        size="lg"
        :ok-title="$t('common.action.search')"
        hide-header
        :busy="!newPattern"
        :cancel-title="$t('common.action.reset')"
        @cancel="cancel"
        @hidden="SET_CURRENT_SEARCH_PATTERN(newPattern)"
        @ok="validateAndsearch"
    >
        <div class="item">
            <div class="label">{{ $t("mail.search.options.folder.label") }}</div>
            <mail-search-box-context class="input" :folder="currentFolder" />
        </div>
        <h3 class="section">{{ $t("mail.search.section.msg_and_attachments") }}</h3>
        <div class="item">
            <div class="label">{{ $t("mail.search.label.subject") }}</div>
            <advanced-search-input :value.sync="subject" />
        </div>
        <div class="item">
            <div class="label">{{ $t("mail.search.label.contains") }}</div>
            <advanced-search-input :value.sync="contains" />
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
import SearchHelper from "../../SearchHelper";
import AdvancedSearchInput from "./AdvancedSearchInput";

const PATTERN_KEYWORDS = {
    SUBJECT: "subject"
};

export default {
    name: "AdvancedSearchModal",
    components: { BmModal, MailSearchBoxContext, AdvancedSearchInput },
    mixins: [SearchMixin],
    data() {
        return {
            contains: null,
            subject: null
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
            const subParts = Object.values(PATTERN_KEYWORDS).flatMap(keyword =>
                this[keyword] ? `${keyword}:${this[keyword]}` : []
            );
            if (this.contains) {
                subParts.push(this.contains);
            }
            return subParts.join(" ");
        }
    },
    watch: {
        currentPattern: {
            handler() {
                const groups = SearchHelper.parseSearchPattern(this.currentPattern, Object.values(PATTERN_KEYWORDS));
                this.contains = groups.contains;
                this.subject = groups.subject;
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
        },
        validateAndsearch() {
            this.SET_CURRENT_SEARCH_PATTERN(this.newPattern);
            this.search();
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

                & > .label {
                    width: 25%;
                    text-align: end;
                }
                .input {
                    margin-left: $sp-6;
                }
                .mail-search-box-context {
                    width: 20rem;
                }
            }
        }
    }
}
</style>
