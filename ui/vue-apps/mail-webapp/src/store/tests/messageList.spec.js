import Vue from "vue";
import Vuex from "vuex";
import cloneDeep from "lodash.clonedeep";
import { default as storeOptions, ConversationListStatus, ConversationListFilter } from "../conversationList";
import apiMessages from "../api/apiMessages";
import {
    CONVERSATION_LIST_COUNT,
    CONVERSATION_LIST_ALL_KEYS,
    CONVERSATION_LIST_FILTERED,
    CONVERSATION_LIST_FLAGGED_FILTER_ENABLED,
    CONVERSATION_LIST_IS_LOADING,
    CONVERSATION_LIST_IS_REJECTED,
    CONVERSATION_LIST_IS_RESOLVED,
    CONVERSATION_LIST_KEYS,
    CONVERSATION_LIST_TOTAL_PAGES,
    CONVERSATION_LIST_HAS_NEXT,
    CONVERSATION_LIST_UNREAD_FILTER_ENABLED
} from "~/getters";
import { CONVERSATION_LIST_NEXT_PAGE, FETCH_CONVERSATION_LIST_KEYS } from "~/actions";
import {
    ADD_MESSAGES,
    CLEAR_CONVERSATION_LIST,
    REMOVE_MESSAGES,
    MOVE_MESSAGES,
    RESET_CONVERSATION_LIST_PAGE,
    SET_CONVERSATION_LIST,
    SET_CONVERSATION_LIST_FILTER,
    SET_CONVERSATION_LIST_PAGE,
    SET_CONVERSATION_LIST_STATUS,
    SET_SEARCH_PATTERN,
    REMOVE_CONVERSATIONS
} from "~/mutations";
jest.mock("../api/apiMessages");
Vue.use(Vuex);

describe("conversationList", () => {
    describe("actions", () => {
        let store;
        beforeEach(() => {
            store = new Vuex.Store(cloneDeep(storeOptions));
        });
        test("REFRESH_CONVERSATION_LIST_KEYS list messages", async () => {
            const ids = [1, 2, 3, 5, 7, 11, 13];
            apiMessages.sortedIds.mockResolvedValueOnce(ids);
            await store.dispatch(FETCH_CONVERSATION_LIST_KEYS, {
                folder: { key: "key", remoteRef: { uid: "uid" } }
            });
            expect(apiMessages.sortedIds).toHaveBeenCalled();
            expect(store.state._keys.length).toEqual(ids.length);
        });
        test("REFRESH_CONVERSATION_LIST_KEYS search messages", async () => {
            store.commit(SET_SEARCH_PATTERN, "Search pattern");
            const ids = [1, 2, 3, 5, 7, 11, 13].map(id => ({ id, folderRef: {} }));
            apiMessages.search.mockResolvedValueOnce(ids);
            await store.dispatch(FETCH_CONVERSATION_LIST_KEYS, {
                folder: { key: "key", remoteRef: { uid: "uid" } }
            });
            expect(apiMessages.search).toHaveBeenCalled();
            expect(store.state._keys.length).toEqual(ids.length);
        });
        test("REFRESH_CONVERSATION_LIST_KEYS failure", async () => {
            apiMessages.sortedIds.mockRejectedValue("Error");
            expect.assertions(1);
            try {
                await store.dispatch(FETCH_CONVERSATION_LIST_KEYS, {
                    folder: { key: "key", remoteRef: { uid: "uid" } }
                });
            } catch (e) {
                expect(e).toEqual("Error");
            }
        });
        test("FETCH_CONVERSATION_LIST_KEYS list messages", async () => {
            const ids = [1, 2, 3, 5, 7, 11, 13];
            apiMessages.sortedIds.mockResolvedValueOnce(ids);
            const promise = store.dispatch(FETCH_CONVERSATION_LIST_KEYS, {
                folder: { key: "key", remoteRef: { uid: "uid" } }
            });
            expect(store.state.status).toEqual(ConversationListStatus.LOADING);
            expect(apiMessages.sortedIds).toHaveBeenCalled();
            await promise;
            expect(store.state.status).toEqual(ConversationListStatus.SUCCESS);
            expect(store.state._keys.length).toEqual(ids.length);
        });
        test("FETCH_CONVERSATION_LIST_KEYS search messages", async () => {
            store.commit(SET_SEARCH_PATTERN, "Search pattern");
            const ids = [1, 2, 3, 5, 7, 11, 13].map(id => ({ id, folderRef: {} }));
            apiMessages.search.mockResolvedValueOnce(ids);
            const promise = store.dispatch(FETCH_CONVERSATION_LIST_KEYS, {
                folder: { key: "key", remoteRef: { uid: "uid" } }
            });
            expect(store.state.status).toEqual(ConversationListStatus.LOADING);
            expect(apiMessages.search).toHaveBeenCalled();
            await promise;
            expect(store.state.status).toEqual(ConversationListStatus.SUCCESS);
            expect(store.state._keys.length).toEqual(ids.length);
        });
        test("FETCH_CONVERSATION_LIST_KEYS failure", async () => {
            apiMessages.sortedIds.mockRejectedValue("Error");
            try {
                await store.dispatch(FETCH_CONVERSATION_LIST_KEYS, {
                    folder: { key: "key", remoteRef: { uid: "uid" } }
                });
            } catch (e) {
                expect(e).toEqual("Error");
            } finally {
                expect(store.state.status).toEqual(ConversationListStatus.ERROR);
            }
        });
        test("CONVERSATION_LIST_NEXT_PAGE next page exist", async () => {
            store.state._keys = Array(100)
                .fill(0)
                .map((v, i) => i);
            await store.dispatch(CONVERSATION_LIST_NEXT_PAGE);
            expect(store.state.currentPage).toBe(1);
            await store.dispatch(CONVERSATION_LIST_NEXT_PAGE);
            expect(store.state.currentPage).toBe(2);
        });

        test("CONVERSATION_LIST_NEXT_PAGE next page does not exist", async () => {
            store.state._keys = Array(100)
                .fill(0)
                .map((v, i) => i);
            store.state.currentPage = 2;
            await store.dispatch(CONVERSATION_LIST_NEXT_PAGE);
            expect(store.state.currentPage).toBe(2);
        });
    });
    describe("mutations", () => {
        let state;
        let conversation = {
            key: 1,
            folderRef: {
                key: "inbox"
            },
            messages: [
                { key: 1, folderRef: { key: "inbox" } },
                { key: 2, folderRef: { key: "sent" } },
                { key: 3, folderRef: { key: "inbox" } }
            ]
        };
        beforeEach(() => {
            state = cloneDeep(storeOptions.state);
        });
        test("REMOVE_CONVERSATIONS", () => {
            state._keys = [1, 2, 3, 4];
            storeOptions.mutations[REMOVE_CONVERSATIONS](state, [{ key: 3 }, { key: 4 }, { key: 5 }]);
            expect(state._keys).toEqual([1, 2, 3, 4]);
            expect(state._removed).toEqual([3, 4]);
        });
        test("REMOVE_MESSAGES but not conversation", () => {
            state._keys = [1, 2, 3, 4];
            storeOptions.mutations[REMOVE_MESSAGES](state, {
                conversation,
                messages: [{ key: 1 }, { key: 2 }]
            });
            expect(state._keys).toEqual([1, 2, 3, 4]);
            expect(state._removed).toEqual([]);
        });
        test("REMOVE_MESSAGES then conversation", () => {
            state._keys = [1, 2, 3, 4];
            storeOptions.mutations[REMOVE_MESSAGES](state, {
                conversation,
                messages: [{ key: 1 }, { key: 3 }]
            });
            expect(state._keys).toEqual([1, 2, 3, 4]);
            expect(state._removed).toEqual([1]);
        });
        test("MOVE_MESSAGES but not conversation", () => {
            state._keys = [1, 2, 3, 4];
            state._removed = [3];
            storeOptions.mutations[MOVE_MESSAGES](state, {
                conversation,
                messages: [{ key: 1 }, { key: 2 }]
            });
            expect(state._keys).toEqual([1, 2, 3, 4]);
            expect(state._removed).toEqual([3]);
        });
        test("MOVE_MESSAGES then conversation", () => {
            state._keys = [1, 2, 3, 4];
            state._removed = [3];
            storeOptions.mutations[MOVE_MESSAGES](state, {
                conversation,
                messages: [{ key: 1 }, { key: 3 }]
            });
            expect(state._keys).toEqual([1, 2, 3, 4]);
            expect(state._removed).toEqual([3, 1]);
        });
        test("SET_CONVERSATION_LIST", () => {
            state._keys = [1, 2, 3];
            state._removed = [2];
            storeOptions.mutations[SET_CONVERSATION_LIST](state, [{ key: 3 }, { key: 4 }, { key: 5 }]);
            expect(state._keys).toEqual([3, 4, 5]);
            expect(state._removed).toEqual([]);
        });
        test("CLEAR_CONVERSATION_LIST", () => {
            state._keys = [1, 2, 3, 4];
            state._removed = [2];
            storeOptions.mutations[CLEAR_CONVERSATION_LIST](state);
            expect(state._keys).toEqual([]);
            expect(state._removed).toEqual([]);
        });
        test("SET_CONVERSATION_LIST_STATUS", () => {
            storeOptions.mutations[SET_CONVERSATION_LIST_STATUS](state, "AnyStatus");
            expect(state.status).toEqual("AnyStatus");
        });
        test("SET_CONVERSATION_LIST_FILTER initial state", () => {
            expect(state.filter).toEqual(ConversationListFilter.ALL);
        });
        test("SET_CONVERSATION_LIST_FILTER", () => {
            storeOptions.mutations[SET_CONVERSATION_LIST_FILTER](state, "AnyFilter");
            expect(state.filter).toEqual("AnyFilter");
        });
        test("SET_CONVERSATION_LIST_PAGE", () => {
            storeOptions.mutations[SET_CONVERSATION_LIST_PAGE](state, 5);
            expect(state.currentPage).toEqual(5);
        });
        test("RESET_CONVERSATION_LIST_PAGE", () => {
            storeOptions.mutations[RESET_CONVERSATION_LIST_PAGE](state);
            expect(state.currentPage).toEqual(0);
        });
    });
    describe("getters", () => {
        let state;
        beforeEach(() => {
            state = cloneDeep(storeOptions.state);
        });
        test("CONVERSATION_LIST_IS_LOADING", () => {
            expect(storeOptions.getters[CONVERSATION_LIST_IS_LOADING](state)).toBeTruthy();
            state.status = ConversationListStatus.SUCCESS;
            expect(storeOptions.getters[CONVERSATION_LIST_IS_LOADING](state)).toBeFalsy();
            state.status = ConversationListStatus.LOADING;
            expect(storeOptions.getters[CONVERSATION_LIST_IS_LOADING](state)).toBeTruthy();
            state.status = ConversationListStatus.ERROR;
            expect(storeOptions.getters[CONVERSATION_LIST_IS_LOADING](state)).toBeFalsy();
        });
        test("CONVERSATION_LIST_IS_RESOLVED", () => {
            expect(storeOptions.getters[CONVERSATION_LIST_IS_RESOLVED](state)).toBeFalsy();
            state.status = ConversationListStatus.LOADING;
            expect(storeOptions.getters[CONVERSATION_LIST_IS_RESOLVED](state)).toBeFalsy();
            state.status = ConversationListStatus.SUCCESS;
            expect(storeOptions.getters[CONVERSATION_LIST_IS_RESOLVED](state)).toBeTruthy();
            state.status = ConversationListStatus.ERROR;
            expect(storeOptions.getters[CONVERSATION_LIST_IS_RESOLVED](state)).toBeFalsy();
        });
        test("CONVERSATION_LIST_IS_REJECTED", () => {
            expect(storeOptions.getters[CONVERSATION_LIST_IS_REJECTED](state)).toBeFalsy();
            state.status = ConversationListStatus.LOADING;
            expect(storeOptions.getters[CONVERSATION_LIST_IS_REJECTED](state)).toBeFalsy();
            state.status = ConversationListStatus.SUCCESS;
            expect(storeOptions.getters[CONVERSATION_LIST_IS_REJECTED](state)).toBeFalsy();
            state.status = ConversationListStatus.ERROR;
            expect(storeOptions.getters[CONVERSATION_LIST_IS_REJECTED](state)).toBeTruthy();
        });
        test("CONVERSATION_LIST_FILTERED", () => {
            state.filter = ConversationListFilter.ALL;
            expect(storeOptions.getters[CONVERSATION_LIST_FILTERED](state)).toBeFalsy();
            state.filter = ConversationListFilter.UNREAD;
            expect(storeOptions.getters[CONVERSATION_LIST_FILTERED](state)).toBeTruthy();
            state.filter = ConversationListFilter.FLAGGED;
            expect(storeOptions.getters[CONVERSATION_LIST_FILTERED](state)).toBeTruthy();
        });
        test("CONVERSATION_LIST_UNREAD_FILTER_ENABLED", () => {
            state.filter = ConversationListFilter.ALL;
            expect(storeOptions.getters[CONVERSATION_LIST_UNREAD_FILTER_ENABLED](state)).toBeFalsy();
            state.filter = ConversationListFilter.UNREAD;
            expect(storeOptions.getters[CONVERSATION_LIST_UNREAD_FILTER_ENABLED](state)).toBeTruthy();
            state.filter = ConversationListFilter.FLAGGED;
            expect(storeOptions.getters[CONVERSATION_LIST_UNREAD_FILTER_ENABLED](state)).toBeFalsy();
        });
        test("CONVERSATION_LIST_FLAGGED_FILTER_ENABLED", () => {
            state.filter = ConversationListFilter.ALL;
            expect(storeOptions.getters[CONVERSATION_LIST_FLAGGED_FILTER_ENABLED](state)).toBeFalsy();
            state.filter = ConversationListFilter.UNREAD;
            expect(storeOptions.getters[CONVERSATION_LIST_FLAGGED_FILTER_ENABLED](state)).toBeFalsy();
            state.filter = ConversationListFilter.FLAGGED;
            expect(storeOptions.getters[CONVERSATION_LIST_FLAGGED_FILTER_ENABLED](state)).toBeTruthy();
        });
        test("CONVERSATION_LIST_HAS_NEXT", () => {
            state.currentPage = 4;
            expect(
                storeOptions.getters[CONVERSATION_LIST_HAS_NEXT](state, { CONVERSATION_LIST_TOTAL_PAGES: 5 })
            ).toBeTruthy();
            state.currentPage = 5;
            expect(
                storeOptions.getters[CONVERSATION_LIST_HAS_NEXT](state, { CONVERSATION_LIST_TOTAL_PAGES: 5 })
            ).toBeFalsy();
            state.currentPage = 6;
            expect(
                storeOptions.getters[CONVERSATION_LIST_HAS_NEXT](state, { CONVERSATION_LIST_TOTAL_PAGES: 5 })
            ).toBeFalsy();
            state.currentPage = 0;
            expect(
                storeOptions.getters[CONVERSATION_LIST_HAS_NEXT](state, { CONVERSATION_LIST_TOTAL_PAGES: 0 })
            ).toBeFalsy();
        });
        test("CONVERSATION_LIST_KEYS", () => {
            let CONVERSATION_LIST_ALL_KEYS = Array(210)
                .fill(0)
                .map((v, i) => i);
            state.currentPage = 1;
            expect(storeOptions.getters[CONVERSATION_LIST_KEYS](state, { CONVERSATION_LIST_ALL_KEYS }).length).toEqual(
                50
            );
            expect(storeOptions.getters[CONVERSATION_LIST_KEYS](state, { CONVERSATION_LIST_ALL_KEYS })).toEqual(
                CONVERSATION_LIST_ALL_KEYS.slice(0, 50)
            );
            state.currentPage = 5;
            expect(storeOptions.getters[CONVERSATION_LIST_KEYS](state, { CONVERSATION_LIST_ALL_KEYS })).toEqual(
                CONVERSATION_LIST_ALL_KEYS.slice(0, 210)
            );
        });
        test("CONVERSATION_LIST_TOTAL_PAGES", () => {
            let CONVERSATION_LIST_ALL_KEYS = Array(210);
            expect(
                storeOptions.getters[CONVERSATION_LIST_TOTAL_PAGES](undefined, { CONVERSATION_LIST_ALL_KEYS })
            ).toEqual(5);
            CONVERSATION_LIST_ALL_KEYS = Array(1);
            expect(
                storeOptions.getters[CONVERSATION_LIST_TOTAL_PAGES](undefined, { CONVERSATION_LIST_ALL_KEYS })
            ).toEqual(1);
        });

        test("CONVERSATION_LIST_COUNT", () => {
            let CONVERSATION_LIST_ALL_KEYS = [];
            expect(storeOptions.getters[CONVERSATION_LIST_COUNT](undefined, { CONVERSATION_LIST_ALL_KEYS })).toEqual(0);
            CONVERSATION_LIST_ALL_KEYS = Array.from(Array(5)).map((v, key) => key);
            expect(storeOptions.getters[CONVERSATION_LIST_COUNT](undefined, { CONVERSATION_LIST_ALL_KEYS })).toEqual(5);
        });
        test("CONVERSATION_LIST_ALL_KEYS", () => {
            state._keys = Array(200)
                .fill(0)
                .map((v, i) => i);
            expect(storeOptions.getters[CONVERSATION_LIST_ALL_KEYS](state)).toEqual(state._keys);
            state._removed = [4, 10];
            expect(storeOptions.getters[CONVERSATION_LIST_ALL_KEYS](state).indexOf(4)).toEqual(-1);
            expect(storeOptions.getters[CONVERSATION_LIST_ALL_KEYS](state).indexOf(10)).toEqual(-1);
            expect(storeOptions.getters[CONVERSATION_LIST_ALL_KEYS](state).length).toEqual(198);
            state._removed = state._keys;
            expect(storeOptions.getters[CONVERSATION_LIST_ALL_KEYS](state)).toEqual([]);
        });
    });
});
