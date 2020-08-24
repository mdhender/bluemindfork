import mutations from "../../messages/mutations";

describe("messages", () => {
    describe("mutations", () => {
        describe("ADD_MESSAGE/S", () => {
            const { ADD_MESSAGE, ADD_MESSAGES } = mutations;
            test("index by key", () => {
                const state = {};
                const message = {
                    key: 1
                };
                ADD_MESSAGE(state, message);
                expect(state).toEqual({
                    [message.key]: message
                });
            });
            test("add a bunch of messages", () => {
                const state = {};
                const messages = [{ key: 1 }, { key: 2 }, { key: 3 }];
                ADD_MESSAGES(state, messages);
                expect(state).toMatchInlineSnapshot(`
                    Object {
                      "1": Object {
                        "key": 1,
                      },
                      "2": Object {
                        "key": 2,
                      },
                      "3": Object {
                        "key": 3,
                      },
                    }
                `);
            });
            test("add a bunch of messages but without required parameters", () => {
                const state = {};
                const messages = [{ key: 1 }, { key: 2 }, { key: 3 }];
                ADD_MESSAGES(state, messages);
                expect(state).toMatchInlineSnapshot(`
                    Object {
                      "1": Object {
                        "key": 1,
                      },
                      "2": Object {
                        "key": 2,
                      },
                      "3": Object {
                        "key": 3,
                      },
                    }
                `);
            });
        });

        describe("UPDATE_STATUS", () => {
            const { UPDATE_STATUS } = mutations;
            test("new status", () => {
                const state = { 1: { status: "STATUS" } };
                UPDATE_STATUS(state, { key: 1, status: "UPDATED" });
                expect(state).toEqual({ 1: { status: "UPDATED" } });
            });
            test("no previous status", () => {
                const state = {
                    1: {}
                };
                UPDATE_STATUS(state, { key: 1, status: "UPDATED" });
                expect(state).toEqual({ 1: { status: "UPDATED" } });
            });
        });

        describe("READ FLAG", () => {
            const { MARK_AS_READ } = mutations;
            test("just update read flag", () => {
                const state = {
                    1: { data: { prop: "existing", flags: { read: false, important: true } } },
                    2: { data: { flags: { read: false } } }
                };
                MARK_AS_READ(state, { key: 1 });
                expect(state).toMatchInlineSnapshot(`
                    Object {
                      "1": Object {
                        "data": Object {
                          "flags": Object {
                            "important": true,
                            "read": true,
                          },
                          "prop": "existing",
                        },
                      },
                      "2": Object {
                        "data": Object {
                          "flags": Object {
                            "read": false,
                          },
                        },
                      },
                    }
                `);
            });

            test("when key is missing", () => {
                const state = {
                    1: { data: { prop: "existing", flags: { read: false, important: true } } },
                    2: { data: { flags: { read: false } } }
                };
                const initialState = JSON.parse(JSON.stringify(state));
                MARK_AS_READ(state, 0);
                expect(state).toEqual(initialState);
            });
        });
    });
});
