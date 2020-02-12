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

    static past(opt_today) {
        const today = new LocalDate(opt_today);
        return new DateRange(MINIMUM, today);
    }

    static future(opt_today) {
        const today = new LocalDate(opt_today);
        return new DateRange(today, MAXIMUM);
    }
}

const MINIMUM = new LocalDate(0, 0, 1);
const MAXIMUM = new LocalDate(9999, 11, 31);
