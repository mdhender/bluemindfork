import LocalDate from "./LocalDate";

export default class DateRange {
    constructor(start, end) {
        this.start = new LocalDate(start);
        this.end = new LocalDate(end);
    }

    contains(date) {
        return date.valueOf() >= this.start.valueOf() && date.valueOf() < this.end.valueOf();
    }

    static yesterday(opt_today) {
        const today = new LocalDate(opt_today);
        const yesterday = new LocalDate(opt_today).minus(1);
        return new DateRange(yesterday, today);
    }

    static today(opt_today) {
        const today = new LocalDate(opt_today);
        const tomorrow = new LocalDate(opt_today).plus(1);
        return new DateRange(today, tomorrow);
    }

    static thisWeek(opt_today) {
        const dayOfWeek = new LocalDate(opt_today).getWeekDay();
        const start = new LocalDate(opt_today).minus(dayOfWeek);
        const end = new LocalDate(opt_today).plus(7 - dayOfWeek);
        return new DateRange(start, end);
    }

    static lastWeek(opt_today) {
        const dayOfWeek = new LocalDate(opt_today).getWeekDay();
        const start = new LocalDate(opt_today).minus(7 + dayOfWeek);
        const end = new LocalDate(opt_today).minus(dayOfWeek);
        return new DateRange(start, end);
    }

    static thisMonth(opt_today) {
        const today = new LocalDate(opt_today);
        const start = new LocalDate(today.getFullYear(), today.getMonth(), 1);
        const end = new LocalDate(today.getFullYear(), today.getMonth() + 1, 1);
        return new DateRange(start, end);
    }

    static lastMonth(opt_today) {
        const dayOfMonth = new LocalDate(opt_today).getDate();
        let start = new LocalDate(opt_today).minus(dayOfMonth);
        start = start.minus(start.getDate() - 1);
        const end = new LocalDate(opt_today).minus(dayOfMonth - 1);
        return new DateRange(start, end);
    }

    static past(opt_today) {
        const today = new LocalDate(opt_today);
        return new DateRange(MINIMUM, today);
    }

    static future(opt_today) {
        const today = new LocalDate(opt_today);
        return new DateRange(today.plus(1), MAXIMUM);
    }

    /** @return the past months since the start of the current year. */
    static pastMonths(opt_today) {
        const today = new LocalDate(opt_today);
        const pastMonths = [];
        for (let month = today.getMonth() - 1; month >= 0; month--) {
            const start = new LocalDate(today.getFullYear(), month, 1);
            const end = new LocalDate(today.getFullYear(), month + 1, 1);
            pastMonths.push(new DateRange(start, end));
        }
        return pastMonths;
    }

    /** @return the years before the current year, and after olderYear. */
    static pastYears(opt_today, olderYear = 1970) {
        const today = new LocalDate(opt_today);
        const pastYears = [];
        for (let year = today.getFullYear() - 1; year >= olderYear; year--) {
            const start = new LocalDate(year, 0, 1);
            const end = new LocalDate(year + 1, 0, 1);
            pastYears.push(new DateRange(start, end));
        }
        return pastYears;
    }
}

const MINIMUM = new LocalDate(0, 0, 1);
const MAXIMUM = new LocalDate(9999, 11, 31);
