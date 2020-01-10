import DateRange from "../src/DateRange";
import injector from "@bluemind/inject";

jest.mock("@bluemind/inject", () => {
    let fdow = 1;
    return {
        getProvider() {
            return {
                set: (val) => {
                    fdow = val;
                },
                get: () => {
                    return { firstDayOfWeek: fdow };
                }
            };
        }
    };
});

describe("DateRange", () => {
    beforeEach(() => {
        injector.getProvider().set(1); // default fdow is monday
    }),
    test("contains start date", () => {
        const d1 = new Date("2019-01-01");
        const d2 = new Date("2019-12-25");
        const range = new DateRange(d1, d2);
        expect(range.contains(d1)).toBe(true);
    }),
    test("do not contains end date", () => {
        const d1 = new Date("2019-01-01");
        const d2 = new Date("2019-12-25");
        const range = new DateRange(d1, d2);
        expect(range.contains(d2)).toBe(false);
    }),
    test("contains all date between begin and start", () => {
        const d1 = new Date("2019-01-01");
        const d2 = new Date("2019-01-06");
        const range = new DateRange(d1, d2);
        expect(range.contains(new Date("2019-01-02"))).toBe(true);
        expect(range.contains(new Date("2019-01-03"))).toBe(true);
        expect(range.contains(new Date("2019-01-04"))).toBe(true);
        expect(range.contains(new Date("2019-01-05"))).toBe(true);
    }),
    test("do not contains dates before start and after end", () => {
        const d1 = new Date("2018-01-02");
        const d2 = new Date("2019-01-06");
        const range = new DateRange(d1, d2);
        expect(range.contains(new Date("2018-01-01"))).toBe(false);
        expect(range.contains(new Date("2017-12-31"))).toBe(false);
        expect(range.contains(new Date("2019-01-07"))).toBe(false);
        expect(range.contains(new Date("2020-01-05"))).toBe(false);
    }),
    test("ignore times for start and end", () => {
        const d1 = new Date("2019-01-02 12:00:00");
        const d2 = new Date("2019-01-06 12:00:00");
        const range = new DateRange(d1, d2);
        expect(range.contains(new Date("2019-01-02"))).toBe(true);
        expect(range.contains(new Date("2019-01-06"))).toBe(false);
    }),
    test("today range to match the day", () => {
        const today = new Date("2019-01-01");
        expect(DateRange.today(today).contains(new Date("2019-01-01"))).toBe(true);
        expect(DateRange.today(today).contains(new Date("2018-12-31"))).toBe(false);
        expect(DateRange.today(today).contains(new Date("2019-01-02"))).toBe(false);
    }),
    test("yesterday range to match the day before", () => {
        const today = new Date("2019-01-01");
        expect(DateRange.yesterday(today).contains(new Date("2018-12-31"))).toBe(true);
        expect(DateRange.today(today).contains(new Date("2018-12-30"))).toBe(false);
        expect(DateRange.today(today).contains(new Date("2019-01-02"))).toBe(false);
    }),
    test("this week range to match current week (by default, week starts on monday)", () => {
        const today = new Date("2019-01-01");
        const thisWeek = DateRange.thisWeek(today);
        expect(thisWeek.contains(new Date("2018-12-30"))).toBe(false);
        expect(thisWeek.contains(new Date("2018-12-31"))).toBe(true);
        expect(thisWeek.contains(new Date("2019-01-06"))).toBe(true);
        expect(thisWeek.contains(new Date("2019-01-07"))).toBe(false);
    }),
    test("this week range to match current week (force week to start on sunday)", () => {
        injector.getProvider().set(0); // define fdow as sunday
        const today = new Date("2019-01-01");
        const thisWeek = DateRange.thisWeek(today);
        expect(thisWeek.contains(new Date("2018-12-29"))).toBe(false);
        expect(thisWeek.contains(new Date("2018-12-30"))).toBe(true);
        expect(thisWeek.contains(new Date("2019-01-05"))).toBe(true);
        expect(thisWeek.contains(new Date("2019-01-06"))).toBe(false);
    }),
    test("range includes start date and excludes end date", () => {
        const start = new Date("2019-07-23 12:00:00");
        const end = new Date("2019-07-26 12:00:00");
        const range = new DateRange(start, end);
        expect(range.contains(new Date("2019-07-23"))).toBe(true);
        expect(range.contains(new Date("2019-07-25"))).toBe(true);
        expect(range.contains(new Date("2019-07-26"))).toBe(false);
    }),
    test("future range to match all next days", () => {
        const today = new Date("2019-01-01");
        expect(DateRange.future(today).contains(new Date("2018-12-31"))).toBe(false);
        expect(DateRange.future(today).contains(new Date("2019-01-01"))).toBe(true);
        expect(DateRange.future(today).contains(new Date("2020-12-30"))).toBe(true);
    }),
    test("past range to match all previous days", () => {
        const today = new Date("2019-01-01");
        expect(DateRange.past(today).contains(new Date("2019-01-01"))).toBe(false);
        expect(DateRange.past(today).contains(new Date("2018-12-31"))).toBe(true);
        expect(DateRange.past(today).contains(new Date("2015-01-01"))).toBe(true);
    });
});
