<template>
    <conversation-list-separator v-if="needSeparator" class="date-separator" :text="text" />
</template>
<script>
import DateRanges from "./DateRanges";
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
            text: "",
            range: undefined
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

let allSeparators;
let dateRangeIndex;
let separatorAdded;
let dateRanges;

export function init() {
    allSeparators = [];
    dateRangeIndex = undefined;
    separatorAdded = undefined;
    dateRanges = new DateRanges();
}

/** If a new day has come we must update date ranges. */
function checkNewDay() {
    if (!dateRanges.today.contains(new Date())) {
        dateRanges = new DateRanges();
    }
}

function computeVisibilities() {
    dateRangeIndex = 0;
    separatorAdded = false;
    checkNewDay();

    let index = 0;
    let dateSeparator = allSeparators[index];
    while (dateSeparator) {
        computeVisibility(dateSeparator);
        dateSeparator = allSeparators[++index];
    }
}

function computeVisibility(dateSeparator) {
    dateSeparator.needSeparator = false;

    while (!dateRanges.sortedArray[dateRangeIndex].contains(dateSeparator.conversation.date)) {
        separatorAdded = false;
        dateRangeIndex++;
        if (dateRangeIndex === dateRanges.length) {
            dateRangeIndex = 0;
        }
    }

    const range = dateRanges.sortedArray[dateRangeIndex];
    if (!separatorAdded || dateSeparator.index === 0) {
        dateSeparator.text = range.i18n
            ? dateSeparator.$t(range.i18n)
            : range.date
            ? dateSeparator.$d(range.date, range.dateFormat)
            : range.text;
        separatorAdded = true;
        dateSeparator.range = range;

        const previousNeededSepWithSameRange = allSeparators.find(s => s.needSeparator && s.range === range);
        if (previousNeededSepWithSameRange?.index > dateSeparator.index) {
            previousNeededSepWithSameRange.needSeparator = false;
            dateSeparator.needSeparator = true;
        } else if (!previousNeededSepWithSameRange) {
            dateSeparator.needSeparator = true;
        }
    }
}
</script>
