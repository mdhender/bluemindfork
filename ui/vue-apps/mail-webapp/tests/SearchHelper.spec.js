import SearchHelper from "../src/components/SearchHelper";

const context = {
    commit: jest.fn()
};

describe("[Mail-WebappStore][actions][helper] : SearchHelper.parseQuery", () => {
    beforeEach(() => {
        context.commit.mockClear();
    });
    test("parse a query with a single word pattern and no folder", () => {
        expect(SearchHelper.parseQuery("mybeautifulpattern")).toEqual({
            pattern: "mybeautifulpattern"
        });
    });
    test("parse a query with a multiple words pattern and no folder", () => {
        expect(SearchHelper.parseQuery("my beautiful pattern")).toEqual({ pattern: "my", folder: undefined });
    });
    test("parse a query with a quoted multiple words pattern and no folder", () => {
        expect(SearchHelper.parseQuery('"my beautiful pattern"')).toEqual({
            pattern: "my beautiful pattern"
        });
    });
    test("parse a query with a pattern and a folder", () => {
        expect(SearchHelper.parseQuery("mybeautifulpattern in:myhugefolder")).toEqual({
            pattern: "mybeautifulpattern",
            folder: "myhugefolder"
        });
    });
    test("parse a query with a pattern and a folder", () => {
        expect(SearchHelper.parseQuery("mybeautifulpattern toto tata in:myhugefolder plop")).toEqual({
            pattern: "mybeautifulpattern",
            folder: "myhugefolder"
        });
    });
    test("parse a query with a pattern and a folder and deep", () => {
        expect(SearchHelper.parseQuery("mybeautifulpattern toto tata in:myhugefolder plop is:deep")).toEqual({
            pattern: "mybeautifulpattern",
            folder: "myhugefolder",
            deep: true
        });
    });
});

describe("[Mail-WebappStore][actions][helper] : SearchHelper.isSameSearch", () => {
    beforeEach(() => {
        context.commit.mockClear();
    });
    test("parse a query with a single word pattern and no folder", () => {
        expect(SearchHelper.parseQuery("mybeautifulpattern")).toEqual({
            pattern: "mybeautifulpattern",
            folder: undefined
        });
    });
    test("parse a query with a multiple words pattern and no folder", () => {
        expect(SearchHelper.parseQuery("my beautiful pattern")).toEqual({ pattern: "my", folder: undefined });
    });
    test("parse a query with a quoted multiple words pattern and no folder", () => {
        expect(SearchHelper.parseQuery('"my beautiful pattern"')).toEqual({
            pattern: "my beautiful pattern",
            folder: undefined
        });
    });
    test("parse a query with a pattern and a folder", () => {
        expect(SearchHelper.parseQuery("mybeautifulpattern in:myhugefolder")).toEqual({
            pattern: "mybeautifulpattern",
            folder: "myhugefolder"
        });
    });
    test("parse a query with a pattern and a folder", () => {
        expect(SearchHelper.parseQuery("mybeautifulpattern toto tata in:myhugefolder plop")).toEqual({
            pattern: "mybeautifulpattern",
            folder: "myhugefolder"
        });
    });
});
