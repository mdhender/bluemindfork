import Vue from "vue";
import Vuex from "vuex";
import cloneDeep from "lodash.clonedeep";
import inject from "@bluemind/inject";
import { MockUserIdentitiesClient } from "@bluemind/test-utils";
import storeOptions from "../messageCompose";
import { SET_DRAFT_COLLAPSED_CONTENT, SET_DRAFT_EDITOR_CONTENT, SET_SIGNATURE } from "~mutations";
import { FETCH_SIGNATURE } from "~actions";

const mySignature = "My Signature";
Vue.use(Vuex);

describe("messageCompose", () => {
    let store;
    beforeEach(() => {
        store = new Vuex.Store(cloneDeep(storeOptions));
    });
    describe("mutations", () => {
        test("SET_DRAFT_EDITOR_CONTENT", () => {
            store.commit(SET_DRAFT_EDITOR_CONTENT, "Content");
            expect(store.state.editorContent).toEqual("Content");
        });
        test("SET_DRAFT_COLLAPSED_CONTENT", () => {
            store.commit(SET_DRAFT_COLLAPSED_CONTENT, true);
            expect(store.state.collapsedContent).toBeTruthy();
            store.commit(SET_DRAFT_COLLAPSED_CONTENT, false);
            expect(store.state.collapsedContent).toBeFalsy();
        });
        test("SET_SIGNATURE", () => {
            expect(store.state.signature).toBe("");
            store.commit(SET_SIGNATURE, mySignature);
            expect(store.state.signature).toBe(mySignature);
        });
    });
    describe("actions", () => {
        const userIdentitiesService = new MockUserIdentitiesClient();
        beforeEach(() => {
            inject.register({ provide: "IUserMailIdentities", factory: () => userIdentitiesService });
            userIdentitiesService.getIdentities.mockReturnValue([
                { isDefault: false },
                { isDefault: true, signature: mySignature }
            ]);
        });
        test("FETCH_SIGNATURE", async () => {
            await store.dispatch(FETCH_SIGNATURE);
            expect(userIdentitiesService.getIdentities).toHaveBeenCalled();
            expect(store.state.signature).toBe(mySignature);
        });
    });
});
