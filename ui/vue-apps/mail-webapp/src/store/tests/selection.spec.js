import {
    CLEAR_MESSAGE_LIST,
    REMOVE_MESSAGES,
    SELECT_ALL_MESSAGES,
    SELECT_MESSAGE,
    SET_MESSAGE_LIST,
    UNSELECT_ALL_MESSAGES,
    UNSELECT_MESSAGE
} from "~mutations";
import { MESSAGE_IS_SELECTED, SELECTION_IS_EMPTY, MULTIPLE_MESSAGE_SELECTED, ONE_MESSAGE_SELECTED } from "~getters";
import cloneDeep from "lodash.clonedeep";
import store from "../selection";

describe("selection", () => {
    let state;
    beforeEach(() => {
        state = cloneDeep(store.state);
    });
    describe("mutations", () => {
        test("SET_MESSAGE_LIST", () => {
            state = [1, 2, 3, 4, 5];
            store.mutations[SET_MESSAGE_LIST](state, [{ key: 1 }, { key: 6 }]);
            expect(state).toEqual([1]);
        });
        test("REMOVE_MESSAGES", () => {
            state = [1, 2, 3, 4, 5];
            store.mutations[REMOVE_MESSAGES](state, [1, 3, 6]);
            expect(state).toEqual([2, 4, 5]);
        });
        test("CLEAR_MESSAGE_LIST", () => {
            state = [1, 2, 3, 4, 5];
            store.mutations[CLEAR_MESSAGE_LIST](state);
            expect(state).toEqual([]);
        });
        test("UNSELECT_MESSAGE", () => {
            state = [1, 2, 3, 4, 5];
            store.mutations[UNSELECT_MESSAGE](state, 3);
            expect(state).toEqual([1, 2, 4, 5]);
        });
        test("SELECT_MESSAGE", () => {
            state = [1, 2, 3, 4, 5];
            store.mutations[SELECT_MESSAGE](state, 6);
            expect(state).toEqual([1, 2, 3, 4, 5, 6]);
            store.mutations[SELECT_MESSAGE](state, 6);
            expect(state).toEqual([1, 2, 3, 4, 5, 6]);
        });
        test("SELECT_ALL_MESSAGES", () => {
            state = [1, 2, 3, 4, 5];
            store.mutations[SELECT_ALL_MESSAGES](state, [6, 7, 8, 9]);
            expect(state).toEqual([6, 7, 8, 9]);
        });
        test("SELECT_ALL_MESSAGES: Range max size", done => {
            state = [];
            const keys = Array(2 ** 16 + 1).fill(0);
            try {
                store.mutations[SELECT_ALL_MESSAGES](state, keys);
            } catch (e) {
                done.fail(e);
            }
            expect(state).toEqual(keys);
            done();
        });
        test("UNSELECT_ALL_MESSAGES", () => {
            state = [1, 2, 3, 4, 5];
            store.mutations[UNSELECT_ALL_MESSAGES](state);
            expect(state).toEqual([]);
        });
    });
    describe("getters", () => {
        test("SELECTION_IS_EMPTY", () => {
            state = [1, 2];
            expect(store.getters[SELECTION_IS_EMPTY](state)).toBeFalsy();
            state = [];
            expect(store.getters[SELECTION_IS_EMPTY](state)).toBeTruthy();
        });
        test("MULTIPLE_MESSAGES_SELECTED", () => {
            state = [1, 2];
            expect(store.getters[MULTIPLE_MESSAGE_SELECTED](state)).toBeTruthy();
            state = [1];
            expect(store.getters[MULTIPLE_MESSAGE_SELECTED](state)).toBeFalsy();
            state = [];
            expect(store.getters[MULTIPLE_MESSAGE_SELECTED](state)).toBeFalsy();
        });
        test("ONE_MESSAGE_SELECTED", () => {
            state = [1, 2];
            expect(store.getters[ONE_MESSAGE_SELECTED](state)).toBeFalsy();
            state = [1];
            expect(store.getters[ONE_MESSAGE_SELECTED](state)).toBeTruthy();
            state = [];
            expect(store.getters[ONE_MESSAGE_SELECTED](state)).toBeFalsy();
        });
        test("MESSAGE_IS_SELECTED", () => {
            state = [1, 2];
            expect(store.getters[MESSAGE_IS_SELECTED](state)(1)).toBeTruthy();
            expect(store.getters[MESSAGE_IS_SELECTED](state)(3)).toBeFalsy();
        });
    });
});
