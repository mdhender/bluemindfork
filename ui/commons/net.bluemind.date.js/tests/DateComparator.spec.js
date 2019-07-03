import DateComparator from "../src/DateComparator";

describe("DateComparator", () => {
    test("isSameYear is true only if dates are in the same year", () => {
        expect(DateComparator.isSameYear(new Date("2019-01-01"), new Date("2019-12-31"))).toEqual(true);
        expect(DateComparator.isSameYear(new Date("2019-01-01"), new Date("2018-12-31"))).toEqual(false);
    });

    test("isSameMonth is true only if dates are in the same year and in the same month", () => {
        expect(DateComparator.isSameMonth(new Date("2019-01-01"), new Date("2018-12-31"))).toEqual(false);
        expect(DateComparator.isSameMonth(new Date("2019-01-01"), new Date("2019-01-31"))).toEqual(true);
        expect(DateComparator.isSameMonth(new Date("2019-01-01"), new Date("2018-01-31"))).toEqual(false);
    });

    test("isSameMonth is true only if dates are the exact same day", () => {
        expect(DateComparator.isSameDay(new Date("2019-01-01"), new Date("2019-01-01"))).toEqual(true);
        expect(DateComparator.isSameDay(new Date(2019, 0, 1, 1), new Date(2019, 0, 1, 23))).toEqual(true);
        expect(DateComparator.isSameDay(new Date("2019-01-01"), new Date("2018-12-31"))).toEqual(false);
        expect(DateComparator.isSameDay(new Date("2019-01-01"), new Date("2019-02-01"))).toEqual(false);
        expect(DateComparator.isSameDay(new Date("2019-01-01"), new Date("2018-01-01"))).toEqual(false);
    });

    test("isToday return true for... today..", () => {
        expect(DateComparator.isToday(new Date("2019-01-01"))).toEqual(false);
        const myDate = new Date();
        expect(DateComparator.isToday(myDate)).toEqual(true);
        myDate.setHours(myDate.getHours() == 12 ? 10 : 12);
        expect(DateComparator.isToday(myDate)).toEqual(true);
    });
});
