export default {
    isSameYear(date1, date2) {
        return date1.getFullYear() === date2.getFullYear();
    },

    isSameMonth(date1, date2) {
        return date1.getMonth() === date2.getMonth() && this.isSameYear(date1, date2);
    },

    isSameDay(date1, date2) {
        return date1.getDate() === date2.getDate() && this.isSameMonth(date1, date2);
    },

    isSameDate(date1, date2) {
        return this.isSameDay(date1, date2);
    },

    isToday(date) {
        return this.isSameDate(date, new Date());
    }
};
