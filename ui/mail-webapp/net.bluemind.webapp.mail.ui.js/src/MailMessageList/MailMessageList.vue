<template>
    <bm-list-group
        class="mail-message-list"
        tabindex="0"
        @keyup.delete.prevent="shouldRemoveItem(selectedUid)"
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
                    :to="computeLink(f.item)"
                    style="cursor: pointer;"
                />
            </template>
            <template #loading>
                <mail-message-list-loading style="cursor: pointer;" />
            </template>
        </bm-infinite-scroll>
        <mail-message-list-empty-folder v-else-if="count === 0 && mode == 'default'" />
        <bm-list-group-item v-else-if="mode === 'search'" class="bg-extra-light text-center h-100">
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
import { mapGetters, mapMutations, mapState } from "vuex";
import { DateRange } from "@bluemind/date";
import last from "lodash.last";
import MailMessageListEmptyFolder from "./MailMessageListEmptyFolder";
import MailMessageListHeader from "./MailMessageListHeader";
import MailMessageListItem from "./MailMessageListItem";
import MailMessageListLoading from "./MailMessageListLoading";

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
            position: 0
        };
    },
    computed: {
        ...mapGetters("backend.mail/items", ["messages", "count", "indexOf"]),
        ...mapGetters("backend.mail/folders", { folder: "currentFolder" }),
        ...mapState("backend.mail/items", {
            selectedUid: "current",
            search: "search"
        }),
        mode() {
            return this.search.pattern !== null ? "search" : "default";
        },
        displaySpinner() {
            return this.search.loading === true;
        }
    },
    watch: {
        selectedUid() {
            if (this.selectedUid) {
                this.position = this.indexOf(this.selectedUid);
            }
            if (this.selectedUid && this.$refs["message-" + this.selectedUid]) {
                this.$nextTick(() => {
                    this.$refs["message-" + this.selectedUid];
                    this.$refs["message-" + this.selectedUid].$el.focus();
                });
            }
        },
        folder() {
            this.position = 0;
        }
    },
    created() {
        if (this.selectedUid) {
            this.position = this.indexOf(this.selectedUid);
        }
    },
    methods: {
        ...mapMutations("backend.mail/items", ["shouldRemoveItem"]),
        getRange(date) {
            for (let i = 0; i < RANGES.length; i++) {
                if (RANGES[i].contains(date)) {
                    return RANGES[i];
                }
            }
            return last(RANGES);
        },
        moveTo(diff) {
            if (this.selectedUid) {
                let index = this.indexOf(this.selectedUid) + diff;
                this.goTo(index);
            }
        },
        goTo(index) {
            if (this.selectedUid) {
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
        computeLink(message) {
            if (this.mode === "search") {
                return "/mail/search/" + this.search.pattern + "/" + message.id;
            }
            return "/mail/" + this.folder + "/" + message.id;
        },
        loadMessages(range) {
            this.$store.dispatch("mail-app/loadRange", range);
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
</style>
