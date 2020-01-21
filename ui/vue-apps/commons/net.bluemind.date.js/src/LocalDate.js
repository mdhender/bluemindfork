import injector from "@bluemind/inject";
import isNumber from "lodash.isnumber";
import isObject from "lodash.isobject";

export default class LocalDate {

    constructor(opt_year, month = 0, date = 1) {
        if (isNumber(opt_year)) {
            this.date = buildDate(opt_year, month, date);
            fixDST(this.date, date);
        } else if (isObject(opt_year)) {
            this.date = buildDate(opt_year.getFullYear(), opt_year.getMonth(), opt_year.getDate());
        } else {
            const today = new Date();
            this.date = buildDate(today.getFullYear(), today.getMonth(), today.getDate());
        }
        const env = injector.getProvider("Environment").get();
        if (env) {
            this.firstDayOfWeek = env.firstDayOfWeek;
        }
    }

    minus(days) {
        this.date = add(this.date, -days);
        return this;
    }

    plus(days) {
        this.date = add(this.date, days);
        return this;
    }

    valueOf() {
        return this.date.valueOf();
    }

    getFullYear() {
        return this.date.getFullYear();
    }

    getMonth() {
        return this.date.getMonth();
    }

    getDate() {
        return this.date.getDate();
    }

    getDay() {
        return this.date.getDay();
    }

    getWeekDay() {
        return (this.getDay() + 7 - this.firstDayOfWeek) % 7;
    }

    // 0 = Sunday, 6 = Saturday
    setFirstDayOfWeek(day) {
        this.firstDayOfWeek = day;
    }
}

function buildDate(fullYear, month, date) {
    var d = new Date(fullYear, month, date);
    if (fullYear >= 0 && fullYear < 100) {
        d.setFullYear(d.getFullYear() - 1900);
    }
    return fixDST(d, date);
}

function fixDST(date, expected) {
    if (date.getDate() !== expected) {
        var dir = date.getDate() < expected ? 1 : -1;
        date.setUTCHours(date.getUTCHours() + dir);
    }
    return date;
}

function add(date, days) {
    var noon = new Date(date.getFullYear(), date.getMonth(), date.getDate(), 12);
    var result = new Date(noon.getTime() + days * 86400000);

    // Set date to 1 to prevent rollover caused by setting the year or month.
    date.setDate(1);
    date.setFullYear(result.getFullYear());
    date.setMonth(result.getMonth());
    date.setDate(result.getDate());

    return fixDST(date, result.getDate());
}
