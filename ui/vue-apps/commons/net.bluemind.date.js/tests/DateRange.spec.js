import DateRange from "../src/DateRange";
import injector from "@bluemind/inject";

jest.mock("@bluemind/inject", () => {
    let fdow = 1;
    return {
        getProvider() {
            return {
                set: val => {
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
            expect(DateRange.future(today).contains(new Date("2019-01-01"))).toBe(false);
            expect(DateRange.future(today).contains(new Date("2019-01-02"))).toBe(true);
            expect(DateRange.future(today).contains(new Date("2020-12-30"))).toBe(true);
        }),
        test("past range to match all previous days", () => {
            const today = new Date("2019-01-01");
            expect(DateRange.past(today).contains(new Date("2019-01-01"))).toBe(false);
            expect(DateRange.past(today).contains(new Date("2018-12-31"))).toBe(true);
            expect(DateRange.past(today).contains(new Date("2015-01-01"))).toBe(true);
        }),
        test("match last week", () => {
            const today = new Date("2019-01-01");
            expect(DateRange.lastWeek(today).contains(new Date("2018-12-31"))).toBe(false);
            expect(DateRange.lastWeek(today).contains(new Date("2018-12-30"))).toBe(true);
            expect(DateRange.lastWeek(today).contains(new Date("2018-12-24"))).toBe(true);
            expect(DateRange.lastWeek(today).contains(new Date("2018-12-23"))).toBe(false);
        }),
        test("match this month", () => {
            const today = new Date("2019-01-01");
            expect(DateRange.thisMonth(today).contains(new Date("2018-12-31"))).toBe(false);
            expect(DateRange.thisMonth(today).contains(new Date("2019-01-01"))).toBe(true);
            expect(DateRange.thisMonth(today).contains(new Date("2019-01-31"))).toBe(true);
            expect(DateRange.thisMonth(today).contains(new Date("2019-02-01"))).toBe(false);
        }),
        test("match last month", () => {
            const today = new Date("2019-01-01");
            expect(DateRange.lastMonth(today).contains(new Date("2019-01-01"))).toBe(false);
            expect(DateRange.lastMonth(today).contains(new Date("2018-12-31"))).toBe(true);
            expect(DateRange.lastMonth(today).contains(new Date("2018-12-01"))).toBe(true);
            expect(DateRange.lastMonth(today).contains(new Date("2018-11-30"))).toBe(false);
        }),
        test("match past months", () => {
            const today = new Date("2019-04-01");
            const pastMonths = DateRange.pastMonths(today);

            const feb2019 = pastMonths[1];
            expect(feb2019.contains(new Date("2019-04-01"))).toBe(false);
            expect(feb2019.contains(new Date("2019-03-01"))).toBe(false);
            expect(feb2019.contains(new Date("2019-02-28"))).toBe(true);
            expect(feb2019.contains(new Date("2019-02-01"))).toBe(true);
            expect(feb2019.contains(new Date("2019-01-31"))).toBe(false);

            const jan2019 = pastMonths[2];
            expect(jan2019.contains(new Date("2019-03-01"))).toBe(false);
            expect(jan2019.contains(new Date("2019-02-01"))).toBe(false);
            expect(jan2019.contains(new Date("2019-01-28"))).toBe(true);
            expect(jan2019.contains(new Date("2019-01-01"))).toBe(true);
            expect(jan2019.contains(new Date("2018-12-31"))).toBe(false);
        }),
        test("match past years", () => {
            const today = new Date("2019-01-01");
            const pastYears = DateRange.pastYears(today);

            const year2018 = pastYears[0];
            expect(year2018.contains(new Date("2019-01-01"))).toBe(false);
            expect(year2018.contains(new Date("2018-12-31"))).toBe(true);
            expect(year2018.contains(new Date("2018-01-01"))).toBe(true);
            expect(year2018.contains(new Date("2017-12-31"))).toBe(false);

            const year2016 = pastYears[2];
            expect(year2016.contains(new Date("2017-01-01"))).toBe(false);
            expect(year2016.contains(new Date("2016-12-31"))).toBe(true);
            expect(year2016.contains(new Date("2016-01-01"))).toBe(true);
            expect(year2016.contains(new Date("2015-12-31"))).toBe(false);

            const year1970 = pastYears[pastYears.length - 1];
            expect(year1970.contains(new Date("1971-01-01"))).toBe(false);
            expect(year1970.contains(new Date("1970-12-31"))).toBe(true);
            expect(year1970.contains(new Date("1970-01-01"))).toBe(true);
            expect(year1970.contains(new Date("1969-12-31"))).toBe(false);
        });
});
