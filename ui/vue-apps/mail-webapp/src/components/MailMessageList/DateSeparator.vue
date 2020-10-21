<template>
    <message-list-separator v-if="needSeparator" class="date-separator" :text="text" />
</template>
<script>
import { DateRange } from "@bluemind/date";
import MessageListSeparator from "./MessageListSeparator";

export default {
    name: "DateSeparator",
    components: { MessageListSeparator },
    props: {
        message: {
            type: Object,
            required: true
        },
        force: {
            type: Boolean,
            required: false,
            default: false
        }
    },
    data() {
        return {
            needSeparator: false,
            text: ""
        };
    },
    created() {
        while (!DATE_RANGES[dateRangeIndex].contains(this.message.date)) {
            separatorAdded = false;
            dateRangeIndex++;
            if (dateRangeIndex === DATE_RANGES.length) {
                dateRangeIndex = 0;
            }
        }

        const range = DATE_RANGES[dateRangeIndex];
        if (!separatorAdded || this.force) {
            this.text = range.i18n
                ? this.$t(range.i18n)
                : range.date
                ? this.$d(range.date, range.dateFormat)
                : range.text;
            separatorAdded = true;
            this.needSeparator = true;
        }
    }
};

let dateRangeIndex = 0;
let separatorAdded = false;

/** Warning: keep these ranges mutually exclusives for the global dateRangeIndex to works correctly. */
const FUTURE = DateRange.future();
FUTURE.i18n = "mail.list.range.future";
const TODAY = DateRange.today();
TODAY.i18n = "mail.list.range.today";
const YESTERDAY = DateRange.yesterday();
YESTERDAY.i18n = "mail.list.range.yesterday";
const THIS_WEEK = DateRange.thisWeek();
THIS_WEEK.end = YESTERDAY.start;
THIS_WEEK.i18n = "mail.list.range.this_week";
const LAST_WEEK = DateRange.lastWeek();
LAST_WEEK.i18n = "mail.list.range.last_week";
const THIS_MONTH = DateRange.thisMonth();
THIS_MONTH.end = LAST_WEEK.start;
THIS_MONTH.i18n = "mail.list.range.this_month";
const LAST_MONTH = DateRange.lastMonth();
LAST_MONTH.i18n = "mail.list.range.last_month";
const MONTHS_BEFORE_LAST_MONTH = DateRange.pastMonths()
    .slice(1)
    .map(range => {
        range.date = range.start;
        range.dateFormat = "month";
        return range;
    });
const PAST_YEARS = DateRange.pastYears().map(range => {
    range.text = String(range.start.getFullYear());
    return range;
});
const OLDER = DateRange.past(PAST_YEARS[PAST_YEARS.length - 1].start);
OLDER.i18n = "mail.list.range.older";
const DATE_RANGES = [
    FUTURE,
    TODAY,
    YESTERDAY,
    THIS_WEEK,
    LAST_WEEK,
    THIS_MONTH,
    LAST_MONTH,
    ...MONTHS_BEFORE_LAST_MONTH,
    ...PAST_YEARS,
    OLDER
];
</script>
