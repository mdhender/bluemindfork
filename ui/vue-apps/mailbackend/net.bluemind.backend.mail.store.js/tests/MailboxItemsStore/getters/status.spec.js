import * as gettersstatus from "../../../src/MailboxItemsStore/getters/status";

describe("[MailboxItemsStore]", () => {
    describe("getters", () => {
        const { isLoading, isResolved } = gettersstatus;
        const state = {};
        test("empty messages is loading", () => {
            const messages = undefined;
            expect(isLoading(state, { messages })).toBe(true);
            expect(isResolved(state, { messages })).toBe(false);
        });
        test("null messages is loading", () => {
            const messages = [null];
            expect(isLoading(state, { messages })).toBe(true);
            expect(isResolved(state, { messages })).toBe(false);
        });
        test("non null messages are resolved, so resolved", () => {
            const messages = [1];
            expect(isLoading(state, { messages })).toBe(false);
            expect(isResolved(state, { messages })).toBe(true);
        });
    });
});
