import { searchVCardsHelper } from "../src/";

describe("Search VCard", () => {
    describe("VCardHelper", () => {
        it("build an ESquery with default values", () => {
            expect(searchVCardsHelper("pattern")).toEqual({
                size: 5,
                from: 0,
                orderBy: "Pertinance",
                escapeQuery: false,
                query:
                    "(value.identification.formatedName.value:(pattern) " +
                    "OR value.communications.emails.value:(pattern)) " +
                    "AND ((value.kind:group AND _exists_:value.organizational.member) " +
                    "OR _exists_:value.communications.emails.value)"
            });
        });

        it("options arg is optional", () => {
            expect(searchVCardsHelper("pattern")).toEqual(searchVCardsHelper("pattern", {}));
        });

        it("can set options individualy", () => {
            expect(searchVCardsHelper("pattern", { size: 2, from: 2 })).toEqual({
                size: 2,
                from: 2,
                orderBy: "Pertinance",
                escapeQuery: false,
                query:
                    "(value.identification.formatedName.value:(pattern) " +
                    "OR value.communications.emails.value:(pattern)) " +
                    "AND ((value.kind:group AND _exists_:value.organizational.member) " +
                    "OR _exists_:value.communications.emails.value)"
            });
        });

        it("Accept an array of fields to look for", () => {
            expect(searchVCardsHelper("pattern", { fields: ["name", "location"] }).query).toEqual(
                "(name:(pattern) OR " +
                    "location:(pattern)) AND " +
                    "((value.kind:group AND _exists_:value.organizational.member) OR _exists_:value.communications.emails.value)"
            );
        });
        it("Fields cannot be null", () => {
            expect(() => searchVCardsHelper("pattern", { fields: null }).query).toThrowError();
        });
        it("Fields can be undefined", () => {
            expect(() => searchVCardsHelper("pattern", { fields: undefined }).query).not.toThrowError();
            expect(searchVCardsHelper("pattern", { fields: undefined }).query).toEqual(
                "(value.identification.formatedName.value:(pattern) " +
                    "OR value.communications.emails.value:(pattern)) " +
                    "AND ((value.kind:group AND _exists_:value.organizational.member) " +
                    "OR _exists_:value.communications.emails.value)"
            );
        });
    });
});
