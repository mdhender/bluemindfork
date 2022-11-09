import {
    SET_SELECTION,
    SELECT_CONVERSATION,
    SET_CONVERSATION_LIST,
    UNSELECT_ALL_CONVERSATIONS,
    UNSELECT_CONVERSATION,
    REMOVE_CONVERSATIONS
} from "~/mutations";
import {
    CONVERSATION_IS_SELECTED,
    SEVERAL_CONVERSATIONS_SELECTED,
    ONE_CONVERSATION_SELECTED,
    SELECTION_IS_EMPTY,
    SELECTION_KEYS
} from "~/getters";
import cloneDeep from "lodash.clonedeep";
import store from "../selection";
import { default as storeOptions } from "~/store/conversationList";

jest.mock("postal-mime", () => ({ TextEncoder: jest.fn() }));

describe("selection", () => {
    let state;
    beforeEach(() => {
        state = cloneDeep(store.state);
    });
    describe("mutations", () => {
        test("SET_CONVERSATION_LIST", () => {
            state._keys = [1, 2, 3, 4, 5];
            state._removed = [2, 5];
            store.mutations[SET_CONVERSATION_LIST](state, { conversations: [{ key: 1 }, { key: 2 }, { key: 6 }] });
            expect(state._keys).toEqual([1, 2]);
            expect(state._removed).toEqual([2]);
        });
        test("REMOVE_CONVERSATIONS", () => {
            state._keys = [1, 2, 3, 4];
            storeOptions.mutations[REMOVE_CONVERSATIONS](state, [{ key: 3 }, { key: 4 }, { key: 5 }]);
            expect(state._keys).toEqual([1, 2, 3, 4]);
            expect(state._removed).toEqual([3, 4]);
        });
        test("UNSELECT_CONVERSATION", () => {
            state._keys = [1, 2, 3, 4, 5];
            store.mutations[UNSELECT_CONVERSATION](state, 3);
            expect(state._keys).toEqual([1, 2, 4, 5]);
        });
        test("SELECT_CONVERSATION", () => {
            state._keys = [1, 2, 3, 4, 5];
            store.mutations[SELECT_CONVERSATION](state, 6);
            expect(state._keys).toEqual([1, 2, 3, 4, 5, 6]);
            store.mutations[SELECT_CONVERSATION](state, 6);
            expect(state._keys).toEqual([1, 2, 3, 4, 5, 6]);
        });
        test("SET_SELECTION", () => {
            state._keys = [1, 2, 3, 4, 5];
            store.mutations[SET_SELECTION](state, [6, 7, 8, 9]);
            expect(state._keys).toEqual([6, 7, 8, 9]);
        });
        test("SET_SELECTION: Range max size", done => {
            state._keys = [];
            const keys = Array(2 ** 16 + 1).fill(0);
            try {
                store.mutations[SET_SELECTION](state, keys);
            } catch (e) {
                done.fail(e);
            }
            expect(state._keys).toEqual(keys);
            done();
        });
        test("UNSELECT_ALL_CONVERSATIONS", () => {
            state._keys = [1, 2, 3, 4, 5];
            state._removed = [4];
            store.mutations[UNSELECT_ALL_CONVERSATIONS](state);
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
            expect(store.getters[SEVERAL_CONVERSATIONS_SELECTED](undefined, { SELECTION_KEYS })).toBeTruthy();
            SELECTION_KEYS = [1];
            expect(store.getters[SEVERAL_CONVERSATIONS_SELECTED](undefined, { SELECTION_KEYS })).toBeFalsy();
            SELECTION_KEYS = [];
            expect(store.getters[SEVERAL_CONVERSATIONS_SELECTED](undefined, { SELECTION_KEYS })).toBeFalsy();
        });
        test("ONE_CONVERSATION_SELECTED", () => {
            let SELECTION_KEYS = [1, 2];
            expect(store.getters[ONE_CONVERSATION_SELECTED](undefined, { SELECTION_KEYS })).toBeFalsy();
            SELECTION_KEYS = [1];
            expect(store.getters[ONE_CONVERSATION_SELECTED](undefined, { SELECTION_KEYS })).toBeTruthy();
            SELECTION_KEYS = [];
            expect(store.getters[ONE_CONVERSATION_SELECTED](undefined, { SELECTION_KEYS })).toBeFalsy();
        });
        test("CONVERSATION_IS_SELECTED", () => {
            let SELECTION_KEYS = [1, 2];
            expect(store.getters[CONVERSATION_IS_SELECTED](undefined, { SELECTION_KEYS })(1)).toBeTruthy();
            expect(store.getters[CONVERSATION_IS_SELECTED](undefined, { SELECTION_KEYS })(3)).toBeFalsy();
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
