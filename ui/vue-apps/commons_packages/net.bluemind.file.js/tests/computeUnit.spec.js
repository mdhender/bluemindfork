import computeUnit from "../src/computeUnit.js";
const i18n = {
    t: jest.fn((x, v) => {
        let translate = {
            "common.unit.gigabyte": " Go",
            "common.unit.megabyte": " Mo",
            "common.unit.kilobyte": " Ko",
            "common.unit.byte": " o"
        };
        return v.size + translate[x];
    })
};

describe("Test the rendering with 3 significant digit", () => {
    const dataSet = [
        { octet: 0, result: "0 o" },
        { octet: 1, result: "1 o" },
        { octet: 1 * 1000, result: "1.00 Ko" },
        { octet: 10_000 * 1000, result: "10.0 Mo" },
        { octet: 100_000_000 * 1000, result: "100 Go" },
        { octet: 1_000_000_000 * 1000, result: "1000 Go" }
    ];

    it.each(dataSet)("$octet octets should be rendered as $result", ({ octet, result }) => {
        const settings = { precision: 3 };
        expect(computeUnit(octet, i18n, settings)).toEqual(result);
    });
});

describe("Test the rendering with 2 significant digit", () => {
    const dataSet = [
        { octet: 0, result: "0 o" },
        { octet: 1, result: "1 o" },
        { octet: 1 * 1000, result: "1.0 Ko" },
        { octet: 10_000 * 1000, result: "10 Mo" },
        { octet: 100_000_000 * 1000, result: "100 Go" },
        { octet: 1_000_000_000 * 1000, result: "1000 Go" }
    ];

    it.each(dataSet)("$octet octets should be rendered as $result", ({ octet, result }) => {
        const settings = { precision: 2 };
        expect(computeUnit(octet, i18n, settings)).toEqual(result);
    });
});
describe("Test the rendering with 1 significant digit", () => {
    const dataSet = [
        { octet: 0, result: "0 o" },
        { octet: 1, result: "1 o" },
        { octet: 1 * 1000, result: "1 Ko" },
        { octet: 10_000 * 1000, result: "10 Mo" },
        { octet: 1_546_000 * 1000, result: "2 Go" },
        { octet: 100_000_000 * 1000, result: "100 Go" },
        { octet: 1_000_000_000 * 1000, result: "1000 Go" }
    ];

    it.each(dataSet)("$octet octets should be rendered as $result", ({ octet, result }) => {
        const settings = { precision: 1 };
        expect(computeUnit(octet, i18n, settings)).toEqual(result);
    });
});

describe("Test the rendering with non significant digit specified", () => {
    const dataSet = [
        { octet: 0, result: "0 o" },
        { octet: 1, result: "1 o" },
        { octet: 1 * 1000, result: "1.0 Ko" },
        { octet: 10_000 * 1000, result: "10.0 Mo" },
        { octet: 100_000_000 * 1000, result: "100.0 Go" },
        { octet: 1_000_000_000 * 1000, result: "1000.0 Go" }
    ];

    it.each(dataSet)("$octet octets should be rendered as $result", ({ octet, result }) => {
        const computeResult = computeUnit(octet, i18n);
        expect(computeResult).toEqual(result);
    });
});
