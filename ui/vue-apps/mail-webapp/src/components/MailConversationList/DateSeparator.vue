<template>
    <conversation-list-separator v-if="needSeparator" class="date-separator" :text="text" />
</template>
<script>
import { DateRange } from "@bluemind/date";
import ConversationListSeparator from "./ConversationListSeparator";

export default {
    name: "DateSeparator",
    components: { ConversationListSeparator },
    props: {
        conversation: {
            type: Object,
            required: true
        },
        index: {
            type: Number,
            required: true
        }
    },
    data() {
        return {
            needSeparator: false,
            text: ""
        };
    },
    created() {
        allSeparators.splice(this.index, 0, this);
        computeVisibilities();
    },
    beforeDestroy() {
        allSeparators.splice(this.index, 1);
        computeVisibilities();
    }
};

const allSeparators = [];
let dateRangeIndex;
let separatorAdded;

function computeVisibilities() {
    dateRangeIndex = 0;
    separatorAdded = false;
    let index = 0;
    let dateSeparator = allSeparators[index];
    while (dateSeparator) {
        computeVisibility(dateSeparator);
        dateSeparator = allSeparators[++index];
    }
}

function computeVisibility(dateSeparator) {
    dateSeparator.needSeparator = false;

    while (!DATE_RANGES[dateRangeIndex].contains(dateSeparator.conversation.date)) {
        separatorAdded = false;
        dateRangeIndex++;
        if (dateRangeIndex === DATE_RANGES.length) {
            dateRangeIndex = 0;
        }
    }

    const range = DATE_RANGES[dateRangeIndex];
    if (!separatorAdded || dateSeparator.index === 0) {
        dateSeparator.text = range.i18n
            ? dateSeparator.$t(range.i18n)
            : range.date
            ? dateSeparator.$d(range.date, range.dateFormat)
            : range.text;
        separatorAdded = true;
        dateSeparator.needSeparator = true;
    }
}

/** Warning: keep these ranges mutually exclusives for the global dateRangeIndex to work correctly. */
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
