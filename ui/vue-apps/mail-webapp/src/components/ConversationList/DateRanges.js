import { DateRange } from "@bluemind/date";

export default class DateRanges {
    constructor() {
        this.future = DateRange.future();
        this.today = DateRange.today();
        this.yesterday = DateRange.yesterday();
        this.thisWeek = DateRange.thisWeek();
        this.lastWeek = DateRange.lastWeek();
        this.thisMonth = DateRange.thisMonth();
        this.lastMonth = DateRange.lastMonth();
        this.monthsBeforeLastMonth = DateRange.pastMonths()
            .slice(1)
            .map(range => {
                range.date = range.start;
                range.dateFormat = "month";
                range.i18n = "mail.list.range.past_month";
                return range;
            });
        this.pastYears = DateRange.pastYears().map(range => {
            range.date = range.start;
            range.dateFormat = "year";
            range.i18n = "mail.list.range.past_year";
            return range;
        });

        this.future.i18n = "mail.list.range.future";
        this.today.i18n = "mail.list.range.today";
        this.yesterday.i18n = "mail.list.range.yesterday";
        this.thisWeek.end = this.yesterday.start;
        this.thisWeek.i18n = "mail.list.range.this_week";
        this.lastWeek.i18n = "mail.list.range.last_week";
        this.thisMonth.end = this.lastWeek.start;
        this.thisMonth.i18n = "mail.list.range.this_month";
        this.lastMonth.i18n = "mail.list.range.last_month";
        this.older = DateRange.past(this.pastYears[this.pastYears.length - 1].start);
        this.older.i18n = "mail.list.range.older";
        this.sortedArray = [
            this.future,
            this.today,
            this.yesterday,
            this.thisWeek,
            this.lastWeek,
            this.thisMonth,
            this.lastMonth,
            ...this.monthsBeforeLastMonth,
            ...this.pastYears,
            this.older
        ];
    }
}
