<template>
    <message-list-separator v-if="hasSeparator" :text="$t(range.i18n)" />
</template>

<script>
import { mapGetters, mapState } from "vuex";
import { DateRange } from "@bluemind/date";
import MessageListSeparator from "./MessageListSeparator";
export default {
    components: {
        MessageListSeparator
    },
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapState("mail", { messages: "messages", keys: state => state.messageList.messageKeys }),
        ...mapGetters("mail", ["isLoaded"]),
        range() {
            return Range.getRange(this.message);
        },
        hasSeparator() {
            const previous = this.keys.indexOf(this.message.key) - 1;
            if (previous >= 0) {
                if (this.isLoaded(this.keys[previous])) {
                    return this.range !== Range.getRange(this.messages[this.keys[previous]]);
                }
                return false;
            }
            return true;
        }
    }
};

const Range = (() => {
    const TODAY = DateRange.today();
    TODAY.i18n = "mail.list.range.today";
    const YESTERDAY = DateRange.yesterday();
    YESTERDAY.i18n = "mail.list.range.yesterday";
    const THIS_WEEK = DateRange.thisWeek();
    THIS_WEEK.i18n = "mail.list.range.this_week";
    const OLDER = DateRange.past(THIS_WEEK.start);
    OLDER.i18n = "mail.list.range.older";
    const RANGES = [TODAY, YESTERDAY, THIS_WEEK, OLDER];

    return {
        getRange: ({ date }) => RANGES.find(r => r.contains(date)) || RANGES[RANGES.length - 1]
    };
})();
</script>
