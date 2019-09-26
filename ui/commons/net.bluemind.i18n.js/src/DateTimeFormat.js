import { DateComparator } from "@bluemind/date";

// FIXME with vue-i18n (part "DateTime localization" in the guide)
const dayNames = ["dim.", "lun.", "mar.", "mer.", "jeu.", "ven.", "sam."];

function twoDigit(number) {
    return number.toString().length === 1 ? "0" + number : number;
}

export default {
    getRelativeFormat(date, locale) {
        const today = new Date();
        if (DateComparator.isSameDay(date, today)) {
            return this.formatTime(date, locale);
        } else if (DateComparator.isSameYear(date, today)) {
            return this.formatDayName(date) + " " + this.formatDateWithDayMonth(date, locale);
        }
        return this.formatDate(date, locale);
    },
    formatDate(date, locale) {
        return this.formatDateWithDayMonth(date, locale) + "/" + date.getFullYear();
    },
    formatDateWithDayMonth(date, locale){
        if (locale === 'fr') {
            return twoDigit(date.getDate()) + "/" + twoDigit((date.getMonth() + 1));
        }
        return date.getDate() + "/" + (date.getMonth() + 1);
    },
    formatTime(date, locale) {
        if (locale === 'fr') {
            return twoDigit(date.getHours()) + ":" + twoDigit(date.getMinutes());
        }
        return date.getHours() + ":" + date.getMinutes();
    },
    formatDayName(date) {
        return dayNames[date.getDay()];
    },
    formatDateWithWeekday(date, locale) {
        return this.formatDayName(date) + " " + this.formatDate(date, locale);
    }
};