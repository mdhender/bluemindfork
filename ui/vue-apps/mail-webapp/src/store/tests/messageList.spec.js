import Vue from "vue";
import Vuex from "vuex";
import cloneDeep from "lodash.clonedeep";
import { default as storeOptions, MessageListStatus, MessageListFilter } from "../messageList";
import apiMessages from "../api/apiMessages";
import {
    MESSAGE_LIST_COUNT,
    MESSAGE_LIST_ALL_KEYS,
    MESSAGE_LIST_FILTERED,
    MESSAGE_LIST_FLAGGED_FILTER_ENABLED,
    MESSAGE_LIST_IS_LOADING,
    MESSAGE_LIST_IS_REJECTED,
    MESSAGE_LIST_IS_RESOLVED,
    MESSAGE_LIST_KEYS,
    MESSAGE_LIST_TOTAL_PAGES,
    MESSAGE_LIST_HAS_NEXT,
    MESSAGE_LIST_UNREAD_FILTER_ENABLED
} from "~getters";
import { MESSAGE_LIST_NEXT_PAGE, FETCH_MESSAGE_LIST_KEYS } from "~actions";
import {
    ADD_MESSAGES,
    CLEAR_MESSAGE_LIST,
    REMOVE_MESSAGES,
    MOVE_MESSAGES,
    RESET_MESSAGE_LIST_PAGE,
    SET_MESSAGE_LIST,
    SET_MESSAGE_LIST_FILTER,
    SET_MESSAGE_LIST_PAGE,
    SET_MESSAGE_LIST_STATUS,
    SET_SEARCH_PATTERN
} from "~mutations";
jest.mock("../api/apiMessages");
Vue.use(Vuex);

describe("messageList", () => {
    describe("actions", () => {
        let store;
        beforeEach(() => {
            store = new Vuex.Store(cloneDeep(storeOptions));
        });
        test("REFRESH_MESSAGE_LIST_KEYS list messages", async () => {
            const ids = [1, 2, 3, 5, 7, 11, 13];
            apiMessages.sortedIds.mockResolvedValueOnce(ids);
            await store.dispatch(FETCH_MESSAGE_LIST_KEYS, {
                folder: { key: "key", remoteRef: { uid: "uid" } }
            });
            expect(apiMessages.sortedIds).toHaveBeenCalled();
            expect(store.state._keys.length).toEqual(ids.length);
        });
        test("REFRESH_MESSAGE_LIST_KEYS search messages", async () => {
            store.commit(SET_SEARCH_PATTERN, "Search pattern");
            const ids = [1, 2, 3, 5, 7, 11, 13].map(id => ({ id, folderRef: {} }));
            apiMessages.search.mockResolvedValueOnce(ids);
            await store.dispatch(FETCH_MESSAGE_LIST_KEYS, {
                folder: { key: "key", remoteRef: { uid: "uid" } }
            });
            expect(apiMessages.search).toHaveBeenCalled();
            expect(store.state._keys.length).toEqual(ids.length);
        });
        test("REFRESH_MESSAGE_LIST_KEYS failure", async () => {
            apiMessages.sortedIds.mockRejectedValue("Error");
            expect.assertions(1);
            try {
                await store.dispatch(FETCH_MESSAGE_LIST_KEYS, {
                    folder: { key: "key", remoteRef: { uid: "uid" } }
                });
            } catch (e) {
                expect(e).toEqual("Error");
            }
        });
        test("FETCH_MESSAGE_LIST_KEYS list messages", async () => {
            const ids = [1, 2, 3, 5, 7, 11, 13];
            apiMessages.sortedIds.mockResolvedValueOnce(ids);
            const promise = store.dispatch(FETCH_MESSAGE_LIST_KEYS, {
                folder: { key: "key", remoteRef: { uid: "uid" } }
            });
            expect(store.state.status).toEqual(MessageListStatus.LOADING);
            expect(apiMessages.sortedIds).toHaveBeenCalled();
            await promise;
            expect(store.state.status).toEqual(MessageListStatus.SUCCESS);
            expect(store.state._keys.length).toEqual(ids.length);
        });
        test("FETCH_MESSAGE_LIST_KEYS search messages", async () => {
            store.commit(SET_SEARCH_PATTERN, "Search pattern");
            const ids = [1, 2, 3, 5, 7, 11, 13].map(id => ({ id, folderRef: {} }));
            apiMessages.search.mockResolvedValueOnce(ids);
            const promise = store.dispatch(FETCH_MESSAGE_LIST_KEYS, {
                folder: { key: "key", remoteRef: { uid: "uid" } }
            });
            expect(store.state.status).toEqual(MessageListStatus.LOADING);
            expect(apiMessages.search).toHaveBeenCalled();
            await promise;
            expect(store.state.status).toEqual(MessageListStatus.SUCCESS);
            expect(store.state._keys.length).toEqual(ids.length);
        });
        test("FETCH_MESSAGE_LIST_KEYS failure", async () => {
            apiMessages.sortedIds.mockRejectedValue("Error");
            try {
                await store.dispatch(FETCH_MESSAGE_LIST_KEYS, {
                    folder: { key: "key", remoteRef: { uid: "uid" } }
                });
            } catch (e) {
                expect(e).toEqual("Error");
            } finally {
                expect(store.state.status).toEqual(MessageListStatus.ERROR);
            }
        });
        test("MESSAGE_LIST_NEXT_PAGE next page exist", async () => {
            store.state._keys = Array(100)
                .fill(0)
                .map((v, i) => i);
            await store.dispatch(MESSAGE_LIST_NEXT_PAGE);
            expect(store.state.currentPage).toBe(1);
            await store.dispatch(MESSAGE_LIST_NEXT_PAGE);
            expect(store.state.currentPage).toBe(2);
        });

        test("MESSAGE_LIST_NEXT_PAGE next page does not exist", async () => {
            store.state._keys = Array(100)
                .fill(0)
                .map((v, i) => i);
            store.state.currentPage = 2;
            await store.dispatch(MESSAGE_LIST_NEXT_PAGE);
            expect(store.state.currentPage).toBe(2);
        });
    });
    describe("mutations", () => {
        let state;
        beforeEach(() => {
            state = cloneDeep(storeOptions.state);
        });
        test("REMOVE_MESSAGES", () => {
            state._keys = [1, 2, 3, 4];
            storeOptions.mutations[REMOVE_MESSAGES](state, [{ key: 3 }, { key: 4 }, { key: 5 }]);
            expect(state._keys).toEqual([1, 2, 3, 4]);
            expect(state._removed).toEqual([3, 4]);
        });
        test("MOVE_MESSAGES", () => {
            state._keys = [1, 2, 3, 4];
            state._removed = [3];
            storeOptions.mutations[MOVE_MESSAGES](state, { messages: [{ key: 3 }, { key: 4 }, { key: 5 }] });
            expect(state._keys).toEqual([1, 2, 3, 4]);
            expect(state._removed).toEqual([4]);
        });
        test("ADD_MESSAGES", () => {
            state._keys = [1, 2, 3, 4];
            state._removed = [3, 4];
            storeOptions.mutations[ADD_MESSAGES](state, [{ key: 3 }, { key: 4 }, { key: 5 }]);
            expect(state._keys).toEqual([1, 2, 3, 4]);
            expect(state._removed).toEqual([]);
        });
        test("SET_MESSAGE_LIST", () => {
            state._keys = [1, 2, 3];
            state._removed = [2];
            storeOptions.mutations[SET_MESSAGE_LIST](state, [{ key: 3 }, { key: 4 }, { key: 5 }]);
            expect(state._keys).toEqual([3, 4, 5]);
            expect(state._removed).toEqual([]);
        });
        test("CLEAR_MESSAGE_LIST", () => {
            state._keys = [1, 2, 3, 4];
            state._removed = [2];
            storeOptions.mutations[CLEAR_MESSAGE_LIST](state);
            expect(state._keys).toEqual([]);
            expect(state._removed).toEqual([]);
        });
        test("SET_MESSAGE_LIST_STATUS", () => {
            storeOptions.mutations[SET_MESSAGE_LIST_STATUS](state, "AnyStatus");
            expect(state.status).toEqual("AnyStatus");
        });
        test("SET_MESSAGE_LIST_FILTER initial state", () => {
            expect(state.filter).toEqual(MessageListFilter.ALL);
        });
        test("SET_MESSAGE_LIST_FILTER", () => {
            storeOptions.mutations[SET_MESSAGE_LIST_FILTER](state, "AnyFilter");
            expect(state.filter).toEqual("AnyFilter");
        });
        test("SET_MESSAGE_LIST_PAGE", () => {
            storeOptions.mutations[SET_MESSAGE_LIST_PAGE](state, 5);
            expect(state.currentPage).toEqual(5);
        });
        test("RESET_MESSAGE_LIST_PAGE", () => {
            storeOptions.mutations[RESET_MESSAGE_LIST_PAGE](state);
            expect(state.currentPage).toEqual(0);
        });
    });
    describe("getters", () => {
        let state;
        beforeEach(() => {
            state = cloneDeep(storeOptions.state);
        });
        test("MESSAGE_LIST_IS_LOADING", () => {
            expect(storeOptions.getters[MESSAGE_LIST_IS_LOADING](state)).toBeTruthy();
            state.status = MessageListStatus.SUCCESS;
            expect(storeOptions.getters[MESSAGE_LIST_IS_LOADING](state)).toBeFalsy();
            state.status = MessageListStatus.LOADING;
            expect(storeOptions.getters[MESSAGE_LIST_IS_LOADING](state)).toBeTruthy();
            state.status = MessageListStatus.ERROR;
            expect(storeOptions.getters[MESSAGE_LIST_IS_LOADING](state)).toBeFalsy();
        });
        test("MESSAGE_LIST_IS_RESOLVED", () => {
            expect(storeOptions.getters[MESSAGE_LIST_IS_RESOLVED](state)).toBeFalsy();
            state.status = MessageListStatus.LOADING;
            expect(storeOptions.getters[MESSAGE_LIST_IS_RESOLVED](state)).toBeFalsy();
            state.status = MessageListStatus.SUCCESS;
            expect(storeOptions.getters[MESSAGE_LIST_IS_RESOLVED](state)).toBeTruthy();
            state.status = MessageListStatus.ERROR;
            expect(storeOptions.getters[MESSAGE_LIST_IS_RESOLVED](state)).toBeFalsy();
        });
        test("MESSAGE_LIST_IS_REJECTED", () => {
            expect(storeOptions.getters[MESSAGE_LIST_IS_REJECTED](state)).toBeFalsy();
            state.status = MessageListStatus.LOADING;
            expect(storeOptions.getters[MESSAGE_LIST_IS_REJECTED](state)).toBeFalsy();
            state.status = MessageListStatus.SUCCESS;
            expect(storeOptions.getters[MESSAGE_LIST_IS_REJECTED](state)).toBeFalsy();
            state.status = MessageListStatus.ERROR;
            expect(storeOptions.getters[MESSAGE_LIST_IS_REJECTED](state)).toBeTruthy();
        });
        test("MESSAGE_LIST_FILTERED", () => {
            state.filter = MessageListFilter.ALL;
            expect(storeOptions.getters[MESSAGE_LIST_FILTERED](state)).toBeFalsy();
            state.filter = MessageListFilter.UNREAD;
            expect(storeOptions.getters[MESSAGE_LIST_FILTERED](state)).toBeTruthy();
            state.filter = MessageListFilter.FLAGGED;
            expect(storeOptions.getters[MESSAGE_LIST_FILTERED](state)).toBeTruthy();
        });
        test("MESSAGE_LIST_UNREAD_FILTER_ENABLED", () => {
            state.filter = MessageListFilter.ALL;
            expect(storeOptions.getters[MESSAGE_LIST_UNREAD_FILTER_ENABLED](state)).toBeFalsy();
            state.filter = MessageListFilter.UNREAD;
            expect(storeOptions.getters[MESSAGE_LIST_UNREAD_FILTER_ENABLED](state)).toBeTruthy();
            state.filter = MessageListFilter.FLAGGED;
            expect(storeOptions.getters[MESSAGE_LIST_UNREAD_FILTER_ENABLED](state)).toBeFalsy();
        });
        test("MESSAGE_LIST_FLAGGED_FILTER_ENABLED", () => {
            state.filter = MessageListFilter.ALL;
            expect(storeOptions.getters[MESSAGE_LIST_FLAGGED_FILTER_ENABLED](state)).toBeFalsy();
            state.filter = MessageListFilter.UNREAD;
            expect(storeOptions.getters[MESSAGE_LIST_FLAGGED_FILTER_ENABLED](state)).toBeFalsy();
            state.filter = MessageListFilter.FLAGGED;
            expect(storeOptions.getters[MESSAGE_LIST_FLAGGED_FILTER_ENABLED](state)).toBeTruthy();
        });
        test("MESSAGE_LIST_HAS_NEXT", () => {
            state.currentPage = 4;
            expect(storeOptions.getters[MESSAGE_LIST_HAS_NEXT](state, { MESSAGE_LIST_TOTAL_PAGES: 5 })).toBeTruthy();
            state.currentPage = 5;
            expect(storeOptions.getters[MESSAGE_LIST_HAS_NEXT](state, { MESSAGE_LIST_TOTAL_PAGES: 5 })).toBeFalsy();
            state.currentPage = 6;
            expect(storeOptions.getters[MESSAGE_LIST_HAS_NEXT](state, { MESSAGE_LIST_TOTAL_PAGES: 5 })).toBeFalsy();
            state.currentPage = 0;
            expect(storeOptions.getters[MESSAGE_LIST_HAS_NEXT](state, { MESSAGE_LIST_TOTAL_PAGES: 0 })).toBeFalsy();
        });
        test("MESSAGE_LIST_KEYS", () => {
            let MESSAGE_LIST_ALL_KEYS = Array(210)
                .fill(0)
                .map((v, i) => i);
            state.currentPage = 1;
            expect(storeOptions.getters[MESSAGE_LIST_KEYS](state, { MESSAGE_LIST_ALL_KEYS }).length).toEqual(50);
            expect(storeOptions.getters[MESSAGE_LIST_KEYS](state, { MESSAGE_LIST_ALL_KEYS })).toEqual(
                MESSAGE_LIST_ALL_KEYS.slice(0, 50)
            );
            state.currentPage = 5;
            expect(storeOptions.getters[MESSAGE_LIST_KEYS](state, { MESSAGE_LIST_ALL_KEYS })).toEqual(
                MESSAGE_LIST_ALL_KEYS.slice(0, 210)
            );
        });
        test("MESSAGE_LIST_TOTAL_PAGES", () => {
            let MESSAGE_LIST_ALL_KEYS = Array(210);
            expect(storeOptions.getters[MESSAGE_LIST_TOTAL_PAGES](undefined, { MESSAGE_LIST_ALL_KEYS })).toEqual(5);
            MESSAGE_LIST_ALL_KEYS = Array(1);
            expect(storeOptions.getters[MESSAGE_LIST_TOTAL_PAGES](undefined, { MESSAGE_LIST_ALL_KEYS })).toEqual(1);
        });

        test("MESSAGE_LIST_COUNT", () => {
            let MESSAGE_LIST_ALL_KEYS = [];
            expect(storeOptions.getters[MESSAGE_LIST_COUNT](undefined, { MESSAGE_LIST_ALL_KEYS })).toEqual(0);
            MESSAGE_LIST_ALL_KEYS = Array.from(Array(5)).map((v, key) => key);
            expect(storeOptions.getters[MESSAGE_LIST_COUNT](undefined, { MESSAGE_LIST_ALL_KEYS })).toEqual(5);
        });
        test("MESSAGE_LIST_ALL_KEYS", () => {
            state._keys = Array(200)
                .fill(0)
                .map((v, i) => i);
            expect(storeOptions.getters[MESSAGE_LIST_ALL_KEYS](state)).toEqual(state._keys);
            state._removed = [4, 10];
            expect(storeOptions.getters[MESSAGE_LIST_ALL_KEYS](state).indexOf(4)).toEqual(-1);
            expect(storeOptions.getters[MESSAGE_LIST_ALL_KEYS](state).indexOf(10)).toEqual(-1);
            expect(storeOptions.getters[MESSAGE_LIST_ALL_KEYS](state).length).toEqual(198);
            state._removed = state._keys;
            expect(storeOptions.getters[MESSAGE_LIST_ALL_KEYS](state)).toEqual([]);
        });
    });
});
