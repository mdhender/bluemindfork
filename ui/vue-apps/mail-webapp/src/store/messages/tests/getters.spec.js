import getters from "../../messages/getters";

describe("messages", () => {
    describe("getters", () => {
        test("CURRENT_FOLDER", () => {
            const { ACTIVE_MESSAGES } = getters;
            const state = {
                1: { id: 1, folder: "foo" },
                2: { id: 2, folder: "foo" },
                3: { id: 3, folder: "bar" },
                4: { id: 4, folder: "bar" },
                5: { id: 5, folder: "foo" }
            };
            const ACTIVE_FOLDER = "foo"; // mock getters
            expect(ACTIVE_MESSAGES(state, { ACTIVE_FOLDER })).toEqual([
                { id: 1, folder: "foo" },
                { id: 2, folder: "foo" },
                { id: 5, folder: "foo" }
            ]);
        });
        describe("FILTERED_MESSAGES_BY_FLAG", () => {
            const { FILTERED_MESSAGES_BY_FLAG } = getters;
            test("property missing in messages", () => {
                const state = {
                    1: { id: 1, data: { flags: {} } },
                    2: { id: 2, data: { flags: { read: false } } },
                    3: { id: 3, data: { flags: { read: true } } },
                    4: { id: 3, data: {} }
                };
                expect(FILTERED_MESSAGES_BY_FLAG(state, { ACTIVE_FLAG: { read: false } })).toEqual([
                    { id: 2, data: { flags: { read: false } } }
                ]);
            });
            test("filter unread messages", () => {
                const state = {
                    1: { id: 1, data: { flags: { read: true } } },
                    2: { id: 2, data: { flags: { read: false } } },
                    3: { id: 3, data: { flags: { read: true } } }
                };
                expect(FILTERED_MESSAGES_BY_FLAG(state, { ACTIVE_FLAG: { read: false } })).toEqual([
                    { id: 2, data: { flags: { read: false } } }
                ]);
            });
            test("filter read messages", () => {
                const state = {
                    1: { id: 1, data: { flags: { read: true } } },
                    2: { id: 2, data: { flags: { read: false } } },
                    3: { id: 3, data: { flags: { read: true } } }
                };
                expect(FILTERED_MESSAGES_BY_FLAG(state, { ACTIVE_FLAG: { read: true } })).toEqual([
                    { id: 1, data: { flags: { read: true } } },
                    { id: 3, data: { flags: { read: true } } }
                ]);
            });
            test("filter important messages", () => {
                const state = {
                    1: { id: 1, data: { flags: { important: true } } },
                    2: { id: 2, data: { flags: { important: false } } },
                    3: { id: 3, data: { flags: { important: true } } }
                };
                expect(FILTERED_MESSAGES_BY_FLAG(state, { ACTIVE_FLAG: { important: true } })).toEqual([
                    { id: 1, data: { flags: { important: true } } },
                    { id: 3, data: { flags: { important: true } } }
                ]);
            });
            test("multipe flags", () => {
                const state = {
                    1: { id: 1, data: { flags: { important: true, read: false } } },
                    2: { id: 2, data: { flags: { important: false, read: false } } },
                    3: { id: 3, data: { flags: { important: true, read: true } } },
                    4: { id: 4, data: { flags: { important: false, read: true } } }
                };
                expect(FILTERED_MESSAGES_BY_FLAG(state, { ACTIVE_FLAG: { important: true, read: false } })).toEqual([
                    { id: 1, data: { flags: { important: true, read: false } } }
                ]);
            });
            test("no filter flag given", () => {
                const state = {
                    1: { id: 1, data: { flags: { read: true } } },
                    2: { id: 2, data: { flags: { read: false } } },
                    3: { id: 3, data: { flags: { read: true } } }
                };
                expect(FILTERED_MESSAGES_BY_FLAG(state, {})).toEqual([
                    { id: 1, data: { flags: { read: true } } },
                    { id: 2, data: { flags: { read: false } } },
                    { id: 3, data: { flags: { read: true } } }
                ]);
                expect(FILTERED_MESSAGES_BY_FLAG(state, { ACTIVE_FLAG: {} })).toEqual([
                    { id: 1, data: { flags: { read: true } } },
                    { id: 2, data: { flags: { read: false } } },
                    { id: 3, data: { flags: { read: true } } }
                ]);
            });
        });
    });
});
