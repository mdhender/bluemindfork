import { matchPattern } from "../src";

describe("matchPattern", () => {
    test("is case insensitive, accent insensitive and doesn't have to start with the pattern to be a match, using default options", () => {
        expect(matchPattern("INBOX", "your înbox is full")).toBeTruthy();
        expect(matchPattern("înbÔx", "inbox full")).toBeTruthy();
        expect(matchPattern("inbox", ["folder", "ïnbox 1", "folder 2"])).toBeTruthy();

        expect(matchPattern("not found", "inbox full")).toBeFalsy();
        expect(matchPattern("not found", ["not", "found"])).toBeFalsy();
        expect(matchPattern("not found", ["notfound", "inbox 1", "folder 2"])).toBeFalsy();
    });

    test("do not match on empty content", () => {
        expect(matchPattern("")).toBeFalsy();
        expect(matchPattern("", null)).toBeFalsy();
        expect(matchPattern("", "")).toBeFalsy();
        expect(matchPattern("", [])).toBeFalsy();
        expect(matchPattern("", ["", "", null, undefined, ""])).toBeFalsy();
    });

    test("has options to be case sensitive, accents sensitive, or to only match if the target starts with the pattern", () => {
        expect(matchPattern("inbox", "inbox full", { caseSensitive: true })).toBeTruthy();
        expect(matchPattern("Inbox", "inbox full", { caseSensitive: true })).toBeFalsy();

        expect(matchPattern("éeè", "éeè", { accentsSensitive: true })).toBeTruthy();
        expect(matchPattern("éeè", "èée", { accentsSensitive: true })).toBeFalsy();

        expect(matchPattern("inbox", "inbox full", { startsWith: true })).toBeTruthy();
        expect(matchPattern("inbox", "your inbox is full", { startsWith: true })).toBeFalsy();

        expect(
            matchPattern("Inbox", ["your inbox", "Your Inbox", "inbox"], {
                caseSensitive: true,
                accentsSensitive: true
            })
        ).toBeTruthy();
        expect(
            matchPattern("inbox", ["your inbox", "Inbox", "inbôx"], {
                caseSensitive: true,
                accentsSensitive: true,
                startsWith: true
            })
        ).toBeFalsy();
    });
});
