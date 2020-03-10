import { state, getters, mutations, actions, STATUS } from "../src/modules/search";
import ServiceLocator from "@bluemind/inject";
import { MockMailboxFoldersClient } from "@bluemind/test-mocks";

describe("[Mail-WebappStore][search]", () => {
    describe("state", () => {
        test("initial state", () => {
            expect(state).toEqual({
                pattern: null,
                status: STATUS.IDLE
            });
        });
    });

    describe("mutations", () => {
        const { setPattern, setStatus } = mutations;
        test("setPattern", () => {
            const state = {
                pattern: null
            };
            setPattern(state, "foobar");
            expect(state.pattern).toBe("foobar");
        });

        test("setStatus", () => {
            const state = {
                status: STATUS.IDLE
            };
            setStatus(state, STATUS.LOADING);
            expect(state.status).toBe(STATUS.LOADING);
        });
    });

    describe("getters", () => {
        const { isLoading, isError } = getters;
        test("isLoading", () => {
            const state = {
                status: STATUS.LOADING
            };
            expect(isLoading(state)).toBe(true);
        });
        test("isError", () => {
            const state = {
                status: STATUS.REJECTED
            };
            expect(isError(state)).toBe(true);
        });
    });

    describe("actions", () => {
        const UnrelatedExpectedMutations = [
            {
                type: "mail-webapp/messages/clearItems",
                payload: {
                    root: true
                }
            },
            {
                type: "mail-webapp/currentMessage/clear",
                payload: {
                    root: true
                }
            },
            {
                type: "mail-webapp/messages/clearParts",
                payload: {
                    root: true
                }
            },
            {
                type: "mail-webapp/setMessageFilter",
                payload: {
                    root: true
                }
            }
        ];

        test("search", async () => {
            const mockedImplementation = () =>
                Promise.resolve({
                    results: [
                        {
                            itemId: "foobar"
                        }
                    ]
                });
            mockMailboxFoldersPersistenceSearchItems(mockedImplementation);

            const context = {
                rootState: {
                    ["mail-webapp"]: {
                        currentFolderKey: ""
                    }
                },
                rootGetters: {
                    "mail-webapp/currentMailbox": {
                        mailboxUid: "abcdef"
                    }
                },
                dispatch: jest.fn()
            };

            const expectedMutations = [
                ...UnrelatedExpectedMutations,
                {
                    type: "setStatus",
                    payload: STATUS.LOADING
                },
                {
                    type: "setPattern",
                    payload: "foo"
                },
                {
                    type: "mail-webapp/messages/setItemKeys",
                    payload: ["WyJmb29iYXIiLCIiXQ=="]
                },
                {
                    type: "setStatus",
                    payload: STATUS.RESOLVED
                }
            ];

            await testAction(actions.search, { pattern: "foo" }, context, expectedMutations);
            expect(context.dispatch).toBeCalledWith("mail-webapp/messages/multipleByKey", ["WyJmb29iYXIiLCIiXQ=="], {
                root: true
            });
        });

        test("search is failing", async () => {
            const mockedImplementation = () => Promise.reject("Custom Error!");

            const context = {
                rootState: {
                    ["mail-webapp"]: {
                        currentFolderKey: ""
                    }
                },
                rootGetters: {
                    "mail-webapp/currentMailbox": {
                        mailboxUid: "abcdef"
                    }
                },
                dispatch: jest.fn()
            };
            const expectedMutations = [
                ...UnrelatedExpectedMutations,
                {
                    type: "setStatus",
                    payload: STATUS.LOADING
                },
                {
                    type: "setPattern",
                    payload: "foo"
                },
                {
                    type: "setStatus",
                    payload: STATUS.REJECTED
                }
            ];
            mockMailboxFoldersPersistenceSearchItems(mockedImplementation);
            await testAction(actions.search, { pattern: "foo" }, context, expectedMutations);
        });
    });
});

function mockMailboxFoldersPersistenceSearchItems(searchItemsImplementation) {
    const mockedClient = new MockMailboxFoldersClient();
    mockedClient.searchItems.mockImplementation(searchItemsImplementation);
    ServiceLocator.register({
        provide: "MailboxFoldersPersistence",
        factory: jest.fn().mockReturnValue(mockedClient)
    });
}

// helper for testing action with expected mutations
async function testAction(action, params, context, expectedMutations) {
    let count = 0;

    // mock commit
    const commit = (type, payload) => {
        const mutation = expectedMutations[count];

        expect(type).toEqual(mutation.type);
        if (payload) {
            expect(payload).toEqual(mutation.payload);
        }

        count++;
    };

    // call the action with mocked store and arguments
    const result = await action({ ...context, commit }, params);

    expect(expectedMutations.length).toEqual(count);

    return result;
}
