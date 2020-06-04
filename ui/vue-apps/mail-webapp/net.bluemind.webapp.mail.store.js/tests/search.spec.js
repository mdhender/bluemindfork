import { MockMailboxFoldersClient } from "@bluemind/test-mocks";
import { state, getters, mutations, actions, STATUS } from "../src/modules/search";
import ItemUri from "@bluemind/item-uri";
import ServiceLocator from "@bluemind/inject";

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
        const { isLoading, isResolved, isRejected } = getters;
        test("isLoading", () => {
            const state = {
                status: STATUS.LOADING
            };
            expect(isLoading(state)).toBe(true);
        });
        test("isResolved", () => {
            const state = {
                status: STATUS.RESOLVED
            };
            expect(isResolved(state)).toBe(true);
        });
        test("isRejected", () => {
            const state = {
                status: STATUS.REJECTED
            };
            expect(isRejected(state)).toBe(true);
        });
    });

    describe("actions", () => {
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
                        currentFolderKey: "WyJmMjFhMzU5OS1kODJhLTQ5NTktODEyMi04OWY0ZmFmNjBjMWUiLCJ1c2VyLnRlc3QiXQ=="
                    }
                },
                rootGetters: {
                    "mail-webapp/currentMailbox": {
                        mailboxUid: "abcdef"
                    }
                },
                dispatch: jest.fn(),
                state: {
                    pattern: ""
                }
            };

            const key = ItemUri.encode(
                "foobar",
                ItemUri.item("WyJmMjFhMzU5OS1kODJhLTQ5NTktODEyMi04OWY0ZmFmNjBjMWUiLCJ1c2VyLnRlc3QiXQ==")
            );

            const expectedMutations = [
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
                    payload: [key]
                },
                {
                    type: "setStatus",
                    payload: STATUS.RESOLVED
                }
            ];

            await testAction(actions.search, { pattern: "foo" }, context, expectedMutations);
            expect(context.dispatch).toBeCalledWith("mail-webapp/messages/multipleByKey", [key], {
                root: true
            });
        });

        test("search is failing", async () => {
            const mockedImplementation = () => Promise.reject("Custom Error!");

            const context = {
                rootState: {
                    ["mail-webapp"]: {
                        currentFolderKey: "WyJmMjFhMzU5OS1kODJhLTQ5NTktODEyMi04OWY0ZmFmNjBjMWUiLCJ1c2VyLnRlc3QiXQ=="
                    }
                },
                rootGetters: {
                    "mail-webapp/currentMailbox": {
                        mailboxUid: "abcdef"
                    }
                },
                state: {
                    pattern: ""
                },
                dispatch: jest.fn()
            };
            const expectedMutations = [
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
