import cloneDeep from "lodash.clonedeep";
import store from "../preview";
import { SET_PREVIEW_MESSAGE_KEY, SET_PREVIEW_FILE_KEY } from "~/mutations";

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
        test("SET_PREVIEW_FILE_KEY", () => {
            const fileKey = "partAddress1";
            store.mutations[SET_PREVIEW_FILE_KEY](state, fileKey);
            expect(state.fileKey).toEqual(fileKey);
        });
    });
});
