<template>
    <bm-list-group
        class="mail-message-list"
        tabindex="0"
        @keyup.shift.delete.exact.prevent="openPurgeModal"
        @keyup.delete.exact.prevent="remove"
        @keyup.up="moveTo(-1)"
        @keyup.down="moveTo(+1)"
        @keyup.page-down="moveTo(+PAGE)"
        @keyup.page-up="moveTo(-PAGE)"
        @keyup.home="goTo(0)"
        @keyup.end="goTo(length - 1)"
    >
        <mail-message-list-header />
        <bm-list-group-item v-if="isSearchEnabled" class="font-size-lg pl-4">
            {{ $t("common.search") }} : {{ $tc("common.messages", count, { count }) }}
        </bm-list-group-item>
        <div v-if="isLoading" class="pt-5 text-center">
            {{ $t("common.searching") }}
            <bm-spinner class="pt-3" />
        </div>
        <div v-else-if="count > 0" class="h-100 bg-extra-light mail-message-list__list">
            <div v-for="(message, index) in messages" :key="index">
                <div v-if="!message"><mail-message-list-loading style="cursor: pointer;" /></div>
                <div v-else>
                    <bm-list-group-separator v-if="hasSeparator(message.key)" class="mail-list-separator px-2 py-0">
                        <div class="text-right text-muted font-weight-bold">
                            {{ $t(getSeparator(message.date)) }}
                        </div>
                    </bm-list-group-separator>
                    <mail-message-list-item
                        :ref="'message-' + message.key"
                        :message="message"
                        :to="messageRoute(message.key)"
                        style="cursor: pointer;"
                    />
                </div>
            </div>
        </div>

        <mail-message-list-empty-folder v-else-if="count === 0 && !isSearchEnabled && !areMessagesFiltered" />
        <mail-message-list-empty-filter v-else-if="count === 0 && !isSearchEnabled && areMessagesFiltered" />
        <bm-list-group-item v-else-if="isSearchEnabled" class="bg-extra-light text-center h-100 pr-0">
            <div class="pt-5 font-size-lg">
                <template v-if="isError">
                    {{ $t("common.search.error") }} <br /><br />
                    {{ $t("common.check.connection") }}
                </template>
                <template v-else>
                    {{ $t("mail.list.search.no_result") }} <br />
                    <div class="search-pattern">"{{ search.pattern }}"</div>
                    {{ $t("mail.list.search.no_result.found") }} <br /><br />
                    {{ $t("mail.list.search.no_result.try_otherwise") }}
                    <div
                        class="float-right pt-5 no-search-results-illustration w-50"
                        v-html="noSearchResultsIllustration"
                    />
                </template>
            </div>
        </bm-list-group-item>
    </bm-list-group>
</template>

<script>
import { BmListGroup, BmListGroupItem, BmListGroupSeparator, BmSpinner } from "@bluemind/styleguide";
import { mapGetters, mapState } from "vuex";
import { DateRange } from "@bluemind/date";
import last from "lodash.last";
import MailMessageListEmptyFolder from "./MailMessageListEmptyFolder";
import MailMessageListEmptyFilter from "./MailMessageListEmptyFilter";
import MailMessageListHeader from "./MailMessageListHeader";
import MailMessageListItem from "./MailMessageListItem";
import MailMessageListLoading from "./MailMessageListLoading";
import noSearchResultsIllustration from "../../assets/no-search-result.svg";
import { SHOW_PURGE_MODAL } from "../VueBusEventTypes";

let PAGE_DIFF = 9;

const I18N = Symbol("i18n");
const TODAY = DateRange.today();
TODAY[I18N] = "mail.list.range.today";
const YESTERDAY = DateRange.yesterday();
YESTERDAY[I18N] = "mail.list.range.yesterday";
const THIS_WEEK = DateRange.thisWeek();
THIS_WEEK[I18N] = "mail.list.range.this_week";
const OLDER = DateRange.past(THIS_WEEK.start);
OLDER[I18N] = "mail.list.range.older";
const RANGES = [TODAY, YESTERDAY, THIS_WEEK, OLDER];

export default {
    name: "MailMessageList",
    components: {
        BmListGroup,
        BmListGroupItem,
        BmListGroupSeparator,
        BmSpinner,
        MailMessageListEmptyFolder,
        MailMessageListEmptyFilter,
        MailMessageListHeader,
        MailMessageListItem,
        MailMessageListLoading
    },
    data() {
        return {
            PAGE: PAGE_DIFF,
            noSearchResultsIllustration
        };
    },
    computed: {
        ...mapGetters("mail-webapp", ["nextMessageKey", "my", "areMessagesFiltered"]),
        ...mapGetters("mail-webapp/messages", ["messages", "count", "indexOf"]),
        ...mapState("mail-webapp", ["currentFolderKey", "search", "messageFilter"]),
        ...mapState("mail-webapp/currentMessage", { currentMessageKey: "key" }),
        ...mapGetters("mail-webapp/search", ["isLoading", "isError"]),
        isSearchEnabled() {
            return this.search.pattern;
        }
    },
    methods: {
        remove() {
            if (this.currentFolderKey === this.my.TRASH.key) {
                this.openPurgeModal();
                return;
            }
            this.$router.push(this.messageRoute(this.nextMessageKey));
            this.$store.dispatch("mail-webapp/remove", this.currentMessageKey);
        },
        getRange(date) {
            for (let i = 0; i < RANGES.length; i++) {
                if (RANGES[i].contains(date)) {
                    return RANGES[i];
                }
            }
            return last(RANGES);
        },
        hasSeparator(key) {
            let index = this.indexOf(key);
            if (index > 0) {
                if (
                    !this.messages[index] ||
                    !this.messages[index - 1] ||
                    this.getRange(this.messages[index].date) === this.getRange(this.messages[index - 1].date)
                ) {
                    return false;
                }
            }
            return true;
        },
        getSeparator(date) {
            return this.getRange(date)[I18N];
        },
        messageRoute(key = "") {
            const path = this.$route.path;
            const filter = this.areMessagesFiltered ? "?filter=" + this.messageFilter : "";
            if (this.$route.params.mail) {
                return path.replace(new RegExp("/" + this.$route.params.mail + "/?.*"), "/" + key) + filter;
            } else if (path === "/mail/" || path === "/mail/new") {
                return "/mail/" + this.currentFolderKey + "/" + key + filter;
            }
            return path + key + filter;
        },
        openPurgeModal() {
            this.$bus.$emit(SHOW_PURGE_MODAL);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";

.mail-message-list {
    outline: none;
}

.mail-message-list__list {
    overflow-y: scroll;
}

.mail-message-list .font-size-lg {
    font-size: $font-size-lg;
}

.mail-message-list .search-pattern {
    color: $info-dark;
    font-weight: $font-weight-bold;
    word-break: break-all;
}

.mail-message-list .no-search-results-illustration {
    height: 404px;
    width: 354px;
}

.mail-message-list .list-group-separator {
    padding: $sp-1;
}
</style>
