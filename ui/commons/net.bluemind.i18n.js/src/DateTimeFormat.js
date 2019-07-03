import { DateComparator } from "@bluemind/date";

// FIXME with vue-i18n (part "DateTime localization" in the guide)
const dayNames = ["dim.", "lun.", "mar.", "mer.", "jeu.", "ven.", "sam."];

export default {
    getRelativeFormat(date) {
        const today = new Date();
        if (DateComparator.isSameDay(date, today)) {
            return this.formatHour(date);
        } else if (DateComparator.isSameYear(date, today)) {
            return this.formatDayName(date) + " " + date.getDate() + "/" + (date.getMonth() + 1);
        }
        return this.formatDate(date);
    },
    formatDate(date) {
        return date.getDate() + "/" + (date.getMonth() + 1) + "/" + date.getFullYear();
    },
    formatHour(date) {
        return date.getHours() + ":" + date.getMinutes();
    },
    formatDayName(date) {
        return dayNames[date.getDay()];
    }
};
