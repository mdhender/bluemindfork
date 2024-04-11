import { Quota } from "@bluemind/quota";

describe("Quota class", () => {
    it("should create an instance of Quota with valid parameters", () => {
        const quotaInstance = new Quota({ used: 50, total: 100 });
        expect(quotaInstance).toBeInstanceOf(Quota);
    });
});
describe("Quota getter methods", () => {
    it("should return the correct used and total quota", () => {
        const fakeQuota = { used: 10, total: 100 };
        const quota = new Quota(fakeQuota);
        expect(quota.used).toEqual(fakeQuota.used);
        expect(quota.total).toEqual(fakeQuota.total);
    });
});

describe("Quota threshold methods", () => {
    const dataSet = [
        {
            name: "should be under warning threshold ",
            quotaUsed: 0,
            expectedThreshold: {
                warning: false,
                danger: false,
                unsafe: false,
                critical: false,
                full: false
            }
        },
        {
            name: "should be equal or above warning threshold ",
            quotaUsed: 80,
            expectedThreshold: {
                warning: true,
                danger: false,
                unsafe: false,
                critical: false,
                full: false
            }
        },
        {
            name: "should be equal or above danger threshold ",
            quotaUsed: 90,
            expectedThreshold: {
                warning: true,
                danger: true,
                unsafe: false,
                critical: false,
                full: false
            }
        },
        {
            name: "should be equal or above unsafe threshold ",
            quotaUsed: 95,
            expectedThreshold: {
                warning: true,
                danger: true,
                unsafe: true,
                critical: false,
                full: false
            }
        },
        {
            name: "should be equal or above critical threshold ",
            quotaUsed: 99,
            expectedThreshold: {
                warning: true,
                danger: true,
                unsafe: true,
                critical: true,
                full: false
            }
        },
        {
            name: "should be equal or above full threshold ",
            quotaUsed: 100,
            expectedThreshold: {
                warning: true,
                danger: true,
                unsafe: true,
                critical: true,
                full: true
            }
        }
    ];

    it.each(dataSet)("$name", ({ quotaUsed, expectedThreshold }) => {
        const quota = new Quota({ used: quotaUsed, total: 100 });
        expect(quota.isAboveWarningThreshold()).toBe(expectedThreshold.warning);
        expect(quota.isAboveDangerThreshold()).toBe(expectedThreshold.danger);
        expect(quota.isAboveUnsafeThreshold()).toBe(expectedThreshold.unsafe);
        expect(quota.isAboveCriticalThreshold()).toBe(expectedThreshold.critical);
        expect(quota.isFullyUsed()).toBe(expectedThreshold.full);
    });
});

describe("Quota rendering methods", () => {
    it("should correctly render the used quota percentage", () => {
        expect(new Quota({ used: 25, total: 100 }).usedQuotaPercentage()).toBe(25);
    });
});
