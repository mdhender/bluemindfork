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
            :items="messages"
            :total="count"
            :item-key="'id'"
            item-size="dynamic"
            :goto="position"
            scrollbar
            class="h-100 bg-extra-light"
            @scroll="loadMessages"
        >
            <template #item="f">
                <bm-list-group-separator v-if="hasSeparator(f.item.id)" class="mail-list-separator">
                    <bm-row>
                        <bm-col cols="1" />
                        <bm-col class="pl-3">
                            {{ $t(getSeparator(f.item.date)) }}
                        </bm-col>
                    </bm-row>
                </bm-list-group-separator>
                <mail-message-list-item
                    :ref="'message-' + f.item.id"
                    :message="f.item"
                    :to="messageRoute(f.item.id)"
                    style="cursor: pointer;"
                />
            </template>
            <template #loading>
                <mail-message-list-loading style="cursor: pointer;" />
            </template>
        </bm-infinite-scroll>
        <mail-message-list-empty-folder v-else-if="count === 0 && mode == 'default'" />
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
        MailMessageListHeader,
        MailMessageListItem,
        MailMessageListLoading
    },
    data() {
        return {
            PAGE: PAGE_DIFF,
            noSearchResultsIllustration,
            position: 0
        };
    },
    computed: {
        ...mapGetters("mail-webapp", ["nextMessageId"]),
        ...mapGetters("mail-webapp/messages", ["messages", "count", "indexOf"]),
        ...mapGetters("mail-webapp/folders", ["defaultFolders"]),
        ...mapState("mail-webapp", ["currentFolderUid", "currentMessageId", "search"]),
        mode() {
            return this.search.pattern ? "search" : "default";
        },
        displaySpinner() {
            return this.search.loading === true;
        }
    },
    watch: {
        currentMessageId() {
            if (this.currentMessageId) {
                this.position = this.indexOf(this.currentMessageId);
            }
            if (this.currentMessageId && this.$refs["message-" + this.currentMessageId]) {
                this.$nextTick(() => {
                    this.$refs["message-" + this.currentMessageId];
                    this.$refs["message-" + this.currentMessageId].$el.focus();
                });
            }
        },
        folder() {
            this.position = 0;
        }
    },
    created() {
        if (this.currentMessageId) {
            this.position = this.indexOf(this.currentMessageId);
        }
    },
    methods: {
        ...mapActions("mail-webapp", { loadMessages: "loadRange" }),
        remove() {
            if (this.currentFolderUid == this.defaultFolders.TRASH.uid) {
                this.openPurgeModal();
                return;
            }
            this.$router.push(this.messageRoute(this.nextMessageId));
            this.$store.dispatch("mail-webapp/remove", this.currentMessageId);
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
            if (this.currentMessageId) {
                let index = this.indexOf(this.currentMessageId) + diff;
                this.goTo(index);
            }
        },
        goTo(index) {
            if (this.currentMessageId) {
                index = Math.min(Math.max(0, index), this.count - 1);
                this.$router.push({ path: "" + this.messages[index].id });
            }
        },
        hasSeparator(id) {
            let index = this.indexOf(id);
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
        messageRoute(id) {
            const path = this.$route.path;
            id = id || "";
            if (this.$route.params.mail) {
                return path.replace(new RegExp("/" + this.$route.params.mail + "/?.*"), "/" + id);
            } else if (path == "/mail/" || path == "/mail/new") {
                return "/mail/" + this.currentFolderUid + "/" + id;
            }
            return path + id;
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
</style>
