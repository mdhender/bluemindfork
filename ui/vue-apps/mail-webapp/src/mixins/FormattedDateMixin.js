import { DateComparator } from "@bluemind/date";

export default {
    methods: {
        formatDraftSaveDate(draft) {
            const saveDate = draft.date;
            if (DateComparator.isToday(saveDate)) {
                return { time: this.$d(saveDate, "short_time") };
            }
            return { date: this.$d(saveDate, "short_date") };
        }
    }
};
