import {
    CLEAR_MESSAGE_LIST,
    REMOVE_MESSAGES,
    MOVE_MESSAGES,
    SELECT_ALL_MESSAGES,
    SELECT_MESSAGE,
    SET_MESSAGE_LIST,
    UNSELECT_ALL_MESSAGES,
    UNSELECT_MESSAGE
} from "~mutations";
import {
    MESSAGE_IS_SELECTED,
    MULTIPLE_MESSAGE_SELECTED,
    ONE_MESSAGE_SELECTED,
    SELECTION_IS_EMPTY,
    SELECTION_KEYS
} from "~getters";
import cloneDeep from "lodash.clonedeep";
import store from "../selection";

describe("selection", () => {
    let state;
    beforeEach(() => {
        state = cloneDeep(store.state);
    });
    describe("mutations", () => {
        test("SET_MESSAGE_LIST", () => {
            state._keys = [1, 2, 3, 4, 5];
            state._removed = [2, 5];
            store.mutations[SET_MESSAGE_LIST](state, [{ key: 1 }, { key: 2 }, { key: 6 }]);
            expect(state._keys).toEqual([1, 2]);
            expect(state._removed).toEqual([2]);
        });
        test("REMOVE_MESSAGES", () => {
            state._keys = [1, 2, 3, 4, 5];
            state._removed = [];
            store.mutations[REMOVE_MESSAGES](state, [{ key: 1 }, { key: 3 }, { key: 6 }]);
            expect(state._keys).toEqual([1, 2, 3, 4, 5]);
            expect(state._removed).toEqual([1, 3]);
        });
        test("MOVE_MESSAGES", () => {
            state._keys = [1, 2, 3, 4, 5];
            state._removed = [3, 4];
            store.mutations[MOVE_MESSAGES](state, { messages: [{ key: 1 }, { key: 3 }, { key: 6 }] });
            expect(state._keys).toEqual([1, 2, 3, 4, 5]);
            expect(state._removed).toEqual([4, 1]);
        });
        test("CLEAR_MESSAGE_LIST", () => {
            state._keys = [1, 2, 3, 4, 5];
            state._removed = [3, 4];
            store.mutations[CLEAR_MESSAGE_LIST](state);
            expect(state).toEqual({ _keys: [], _removed: [] });
        });
        test("UNSELECT_MESSAGE", () => {
            state._keys = [1, 2, 3, 4, 5];
            store.mutations[UNSELECT_MESSAGE](state, 3);
            expect(state._keys).toEqual([1, 2, 4, 5]);
        });
        test("SELECT_MESSAGE", () => {
            state._keys = [1, 2, 3, 4, 5];
            store.mutations[SELECT_MESSAGE](state, 6);
            expect(state._keys).toEqual([1, 2, 3, 4, 5, 6]);
            store.mutations[SELECT_MESSAGE](state, 6);
            expect(state._keys).toEqual([1, 2, 3, 4, 5, 6]);
        });
        test("SELECT_ALL_MESSAGES", () => {
            state._keys = [1, 2, 3, 4, 5];
            store.mutations[SELECT_ALL_MESSAGES](state, [6, 7, 8, 9]);
            expect(state._keys).toEqual([6, 7, 8, 9]);
        });
        test("SELECT_ALL_MESSAGES: Range max size", done => {
            state._keys = [];
            const keys = Array(2 ** 16 + 1).fill(0);
            try {
                store.mutations[SELECT_ALL_MESSAGES](state, keys);
            } catch (e) {
                done.fail(e);
            }
            expect(state._keys).toEqual(keys);
            done();
        });
        test("UNSELECT_ALL_MESSAGES", () => {
            state._keys = [1, 2, 3, 4, 5];
            state._removed = [4];
            store.mutations[UNSELECT_ALL_MESSAGES](state);
            expect(state._keys).toEqual([]);
            expect(state._removed).toEqual([]);
        });
    });
    describe("getters", () => {
        test("SELECTION_IS_EMPTY", () => {
            let SELECTION_KEYS = [1, 2];
            expect(store.getters[SELECTION_IS_EMPTY](undefined, { SELECTION_KEYS })).toBeFalsy();
            SELECTION_KEYS = [];
            expect(store.getters[SELECTION_IS_EMPTY](undefined, { SELECTION_KEYS })).toBeTruthy();
        });
        test("MULTIPLE_MESSAGES_SELECTED", () => {
            let SELECTION_KEYS = [1, 2];
            expect(store.getters[MULTIPLE_MESSAGE_SELECTED](undefined, { SELECTION_KEYS })).toBeTruthy();
            SELECTION_KEYS = [1];
            expect(store.getters[MULTIPLE_MESSAGE_SELECTED](undefined, { SELECTION_KEYS })).toBeFalsy();
            SELECTION_KEYS = [];
            expect(store.getters[MULTIPLE_MESSAGE_SELECTED](undefined, { SELECTION_KEYS })).toBeFalsy();
        });
        test("ONE_MESSAGE_SELECTED", () => {
            let SELECTION_KEYS = [1, 2];
            expect(store.getters[ONE_MESSAGE_SELECTED](undefined, { SELECTION_KEYS })).toBeFalsy();
            SELECTION_KEYS = [1];
            expect(store.getters[ONE_MESSAGE_SELECTED](undefined, { SELECTION_KEYS })).toBeTruthy();
            SELECTION_KEYS = [];
            expect(store.getters[ONE_MESSAGE_SELECTED](undefined, { SELECTION_KEYS })).toBeFalsy();
        });
        test("MESSAGE_IS_SELECTED", () => {
            let SELECTION_KEYS = [1, 2];
            expect(store.getters[MESSAGE_IS_SELECTED](undefined, { SELECTION_KEYS })(1)).toBeTruthy();
            expect(store.getters[MESSAGE_IS_SELECTED](undefined, { SELECTION_KEYS })(3)).toBeFalsy();
        });
        test("SELECTION_KEYS", () => {
            state._keys = Array(200)
                .fill(0)
                .map((v, i) => i);
            expect(store.getters[SELECTION_KEYS](state)).toEqual(state._keys);
            state._removed = [4, 10];
            expect(store.getters[SELECTION_KEYS](state).indexOf(4)).toEqual(-1);
            expect(store.getters[SELECTION_KEYS](state).indexOf(10)).toEqual(-1);
            expect(store.getters[SELECTION_KEYS](state).length).toEqual(198);
            state._removed = state._keys;
            expect(store.getters[SELECTION_KEYS](state)).toEqual([]);
        });
    });
});
