import Vue from "vue";
import Vuex from "vuex";
import cloneDeep from "lodash.clonedeep";
import { default as storeOptions, MessageListStatus, MessageListFilter } from "../messageList";
import apiMessages from "../api/apiMessages";
import {
    MESSAGE_LIST_FILTERED,
    MESSAGE_LIST_FLAGGED_FILTER_ENABLED,
    MESSAGE_LIST_IS_LOADING,
    MESSAGE_LIST_IS_REJECTED,
    MESSAGE_LIST_IS_RESOLVED,
    MESSAGE_LIST_UNREAD_FILTER_ENABLED
} from "~getters";
import { FETCH_MESSAGE_LIST_KEYS } from "~actions";
import {
    CLEAR_MESSAGE_LIST,
    REMOVE_MESSAGES,
    MOVE_MESSAGES,
    SET_MESSAGE_LIST,
    SET_MESSAGE_LIST_FILTER,
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
            expect(store.state.messageKeys.length).toEqual(ids.length);
        });
        test("REFRESH_MESSAGE_LIST_KEYS search messages", async () => {
            store.commit(SET_SEARCH_PATTERN, "Search pattern");
            const ids = [1, 2, 3, 5, 7, 11, 13].map(id => ({ id, folderRef: {} }));
            apiMessages.search.mockResolvedValueOnce(ids);
            await store.dispatch(FETCH_MESSAGE_LIST_KEYS, {
                folder: { key: "key", remoteRef: { uid: "uid" } }
            });
            expect(apiMessages.search).toHaveBeenCalled();
            expect(store.state.messageKeys.length).toEqual(ids.length);
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
            expect(store.state.messageKeys.length).toEqual(ids.length);
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
            expect(store.state.messageKeys.length).toEqual(ids.length);
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
    });
    describe("mutations", () => {
        let state;
        beforeEach(() => {
            state = cloneDeep(storeOptions.state);
        });
        test("REMOVE_MESSAGES", () => {
            state.messageKeys = [1, 2, 3, 4];
            storeOptions.mutations[REMOVE_MESSAGES](state, [{ key: 3 }, { key: 4 }, { key: 5 }]);
            expect(state.messageKeys).toEqual([1, 2]);
        });
        test("MOVE_MESSAGES", () => {
            state.messageKeys = [1, 2, 3, 4];
            storeOptions.mutations[MOVE_MESSAGES](state, { messages: [{ key: 3 }, { key: 4 }, { key: 5 }] });
            expect(state.messageKeys).toEqual([1, 2]);
        });
        test("SET_MESSAGE_LIST", () => {
            state.messageKeys = [1, 2, 3];
            storeOptions.mutations[SET_MESSAGE_LIST](state, [{ key: 3 }, { key: 4 }, { key: 5 }]);
            expect(state.messageKeys).toEqual([3, 4, 5]);
        });
        test("CLEAR_MESSAGE_LIST", () => {
            state.messageKeys = [1, 2, 3, 4];
            storeOptions.mutations[CLEAR_MESSAGE_LIST](state);
            expect(state.messageKeys).toEqual([]);
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
    });
});
