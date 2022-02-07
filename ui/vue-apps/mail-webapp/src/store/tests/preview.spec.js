import cloneDeep from "lodash.clonedeep";
import store from "../preview";
import { SET_PREVIEW_MESSAGE_KEY, SET_PREVIEW_PART_ADDRESS } from "~/mutations";

describe("preview", () => {
    let state;
    beforeEach(() => {
        state = cloneDeep(store.state);
    });
    describe("mutations", () => {
        test("SET_PREVIEW_MESSAGE_KEY", () => {
            const messageKey = "messageKey1";
            store.mutations[SET_PREVIEW_MESSAGE_KEY](state, messageKey);
            expect(state.messageKey).toEqual(messageKey);
        });
        test("SET_PREVIEW_PART_ADDRESS", () => {
            const partAddress = "partAddress1";
            store.mutations[SET_PREVIEW_PART_ADDRESS](state, partAddress);
            expect(state.partAddress).toEqual(partAddress);
        });
    });
});
