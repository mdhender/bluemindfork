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
        <bm-list-group-item v-if="mode === 'search'" class="font-size-lg pl-4">
            {{ $t("common.search") }} : {{ $tc("common.messages", count, { count }) }}
        </bm-list-group-item>
        <div v-if="displaySpinner" class="pt-5 text-center">
            {{ $t("common.searching") }}
            <bm-spinner class="pt-3" />
        </div>
        <bm-infinite-scroll
            v-else-if="count > 0"
            ref="bmInfiniteScroll"
            :items="messages"
            :total="count"
            :item-key="'key'"
            item-size="dynamic"
            scrollbar
            class="h-100 bg-extra-light slide-in-from-left"
            @scroll="loadMessages"
        >
            <template #item="f">
                <bm-list-group-separator v-if="hasSeparator(f.item.key)" class="mail-list-separator">
                    <bm-row class="no-gutters pl-2">
                        <bm-col cols="1" />
                        <bm-col>
                            {{ $t(getSeparator(f.item.date)) }}
                        </bm-col>
                    </bm-row>
                </bm-list-group-separator>
                <mail-message-list-item
                    :ref="'message-' + f.item.key"
                    :message="f.item"
                    :to="messageRoute(f.item.key)"
                    style="cursor: pointer;"
                />
            </template>
            <template #loading>
                <mail-message-list-loading style="cursor: pointer;" />
            </template>
        </bm-infinite-scroll>

        <mail-message-list-empty-folder v-else-if="count === 0 && mode == 'default' && !areMessagesFiltered" />
        <mail-message-list-empty-filter v-else-if="count === 0 && mode == 'default' && areMessagesFiltered" />
        <bm-list-group-item v-else-if="mode === 'search'" class="bg-extra-light text-center h-100 pr-0">
            <div class="pt-5 font-size-lg">
                <template v-if="search.error === true">
                    {{ $t("common.search.error") }} <br /><br />
                    {{ $t("common.check.connection") }}
                </template>
                <template v-else>
                    {{ $t("mail.list.search.no_result") }} <br />
                    <div class="search-pattern">"{{ search.pattern }}"</div>
                    {{ $t("mail.list.search.no_result.found") }} <br /><br />
                    {{ $t("mail.list.search.no_result.try_otherwise") }}
                    <!-- eslint-disable vue/no-v-html -->
                    <div
                        class="float-right pt-5 no-search-results-illustration w-50"
                        v-html="noSearchResultsIllustration"
                    />
                    <!-- eslint-enable vue/no-v-html -->
                </template>
            </div>
        </bm-list-group-item>
    </bm-list-group>
</template>

<script>
import {
    BmCol,
    BmListGroup,
    BmListGroupItem,
    BmListGroupSeparator,
    BmInfiniteScroll,
    BmRow,
    BmSpinner
} from "@bluemind/styleguide";
import { mapGetters, mapState, mapActions } from "vuex";
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
        BmCol,
        BmListGroup,
        BmListGroupItem,
        BmListGroupSeparator,
        BmInfiniteScroll,
        BmRow,
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
        ...mapState("mail-webapp", ["currentFolderKey", "currentMessageKey", "search", "messageFilter"]),
        mode() {
            return this.search.pattern ? "search" : "default";
        },
        displaySpinner() {
            return this.search.loading === true;
        }
    },
    watch: {
        currentMessageKey() {
            if (this.currentMessageKey && this.$refs.bmInfiniteScroll) {
                this.$refs.bmInfiniteScroll.goto(this.indexOf(this.currentMessageKey));
            }
            if (this.currentMessageKey && this.$refs["message-" + this.currentMessageKey]) {
                this.$nextTick(() => {
                    this.$refs["message-" + this.currentMessageKey];
                    this.$refs["message-" + this.currentMessageKey].$el.focus();
                });
            }
        },
        currentFolderKey() {
            if (this.$refs.bmInfiniteScroll) {
                this.$refs.bmInfiniteScroll.goto(0);
            }
        }
    },
    created() {
        if (this.currentMessageKey && this.$refs.bmInfiniteScroll) {
            this.$refs.bmInfiniteScroll.goto(this.indexOf(this.currentMessageKey));
        }
    },
    methods: {
        ...mapActions("mail-webapp", { loadMessages: "loadRange" }),
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
        moveTo(diff) {
            if (this.currentMessageKey) {
                let index = this.indexOf(this.currentMessageKey) + diff;
                this.goTo(index);
            }
        },
        goTo(index) {
            if (this.currentMessageKey) {
                index = Math.min(Math.max(0, index), this.count - 1);
                this.$router.push({ path: "" + this.messages[index].key });
            }
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
        messageRoute(key) {
            const path = this.$route.path;
            key = key || "";
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
    padding: map-get($spacers, 1);
    border-bottom: $border-width solid $light;
}
</style>
