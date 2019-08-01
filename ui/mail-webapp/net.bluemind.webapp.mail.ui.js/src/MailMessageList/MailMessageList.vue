<template>
    <bm-list-group
        class="mail-message-list"
        tabindex="0"
        @keyup.up="moveTo(-1)"
        @keyup.down="moveTo(+1)"
        @keyup.page-down="moveTo(+PAGE)"
        @keyup.page-up="moveTo(-PAGE)"
        @keyup.home="goTo(0)"
        @keyup.end="goTo(length - 1)"
    >
        <bm-list-group-item class="bg-transparent align-with-items">
            <bm-row>
                <bm-col cols="1">
                    <bm-check />
                </bm-col>
                <bm-col class="d-none d-sm-block d-md-none d-xl-block pl-2">
                    <span class="text-nowrap">
                        filtrer <span class="fake-select">Tous <bm-icon icon="caret-down" /></span>
                    </span>
                </bm-col>
                <bm-col class="d-none d-sm-block d-md-none d-xl-block text-right pr-0">
                    <span class="text-nowrap">
                        trier par <span class="fake-select">Date <bm-icon icon="caret-down" /></span>
                    </span>
                </bm-col>
            </bm-row>
        </bm-list-group-item>
        <bm-infinite-scroll 
            :items="messages" 
            :position.sync="position" 
            :total="length" 
            :item-key="'uid'" 
            class="h-100 bg-extra-light"
        >
            <template #item="f">
                <bm-list-group-separator v-if="hasSeparator(f.item.uid)" class="mail-list-separator">
                    <bm-row>
                        <bm-col cols="1" />
                        <bm-col class="pl-4">
                            {{ $t(getSeparator(f.item.date)) }}
                        </bm-col>
                    </bm-row>
                </bm-list-group-separator>
                <mail-message-list-item
                    :message="f.item"
                    :to="'/mail/' + folder + '/' + f.item.uid"
                    style="cursor: pointer;"
                />
            </template>
        </bm-infinite-scroll>
    </bm-list-group>
</template>

<script>
import { BmCheck, BmCol, BmListGroup, BmListGroupItem, BmListGroupSeparator, BmIcon, BmInfiniteScroll, BmRow }
    from "@bluemind/styleguide";
import MailMessageListItem from "./MailMessageListItem";
import { mapGetters, mapState } from "vuex";
import { DateRange } from "@bluemind/date";
import findIndex from "lodash.findindex";
import last from "lodash.last";

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
        BmCheck,
        BmCol,
        BmListGroup,
        BmListGroupItem,
        BmListGroupSeparator,
        BmIcon,
        BmInfiniteScroll,
        BmRow,
        MailMessageListItem
    },
    data() {
        return {
            PAGE: PAGE_DIFF,
            position: 0
        };
    },
    computed: {
        ...mapGetters("backend.mail/items", ["messages"]),
        ...mapGetters("backend.mail/folders", { folder: "currentFolder" }),
        ...mapState("backend.mail/items", {
            length: "count",
            selectedUid: "current"
        })
    },
    watch: {
        folder() {
            this.position = -1;
        }
    },
    methods: {
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
                index = Math.min(Math.max(0, index), this.length - 1);
                this.$router.push({ path: "" + this.messages[index].uid });
            }
        },
        indexOf(uid) {
            return findIndex(this.messages, message => message.uid == uid);
        },
        hasSeparator(uid) {
            let index = this.indexOf(uid);
            if (index > 0) {
                if (this.getRange(this.messages[index].date) === this.getRange(this.messages[index - 1].date)) {
                    return false;
                }
            }
            return true;
        },
        getSeparator(date) {
            return this.getRange(date)[I18N];
        }
    }
};
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/variables";
.mail-message-list {
    outline: none;
}
.fake-select {
    color: $info-dark;
    font-weight: $font-weight-bold;
    cursor: pointer;
}
.mail-message-list .align-with-items {
    margin-left: 12px;
}
</style>
