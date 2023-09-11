import Vuex from "vuex";
import cloneDeep from "lodash.clonedeep";
import ServiceLocator, { inject } from "@bluemind/inject";
import { MockMailboxItemsClient } from "@bluemind/test-utils";
import updateMessageStructure from "../../actions/updateMessageStructure";
import initialStore from "../../index";

const key = "key";
const address1 = "111";
const address2 = "222";
const address3 = "333";
const message = {
    key: key,
    folderRef: { uid: "uid" },
    structure: {
        mime: "multipart/mixed",
        children: [
            {
                mime: "multipart/alternative",
                children: [
                    { address: address1, mime: "text/html" },
                    { address: address2, mime: "text/plain" }
                ]
            },
            { address: address3, mime: "image/png" }
        ]
    }
};
const newStructure = {
    mime: "text/html",
    address: "0000"
};

let service, store;

describe("Update message structure action", () => {
    beforeEach(() => {
        ServiceLocator.register({ provide: "MailboxItemsPersistence", use: new MockMailboxItemsClient() });
        service = inject("MailboxItemsPersistence", "uid");
        store = new Vuex.Store(cloneDeep(initialStore));
        store.commit("ADD_MESSAGES", { messages: [message] });
    });
    test("clean obsolete addresses", async () => {
        updateMessageStructure(store, { key, structure: newStructure });
        expect(service.removePart).toHaveBeenCalledWith(address1);
        expect(service.removePart).toHaveBeenCalledWith(address2);
        expect(service.removePart).toHaveBeenCalledWith(address3);
    });

    test("set new structure", async () => {
        updateMessageStructure(store, { key, structure: newStructure });
        const messageInState = store.state[key];
        expect(messageInState.structure).toEqual(newStructure);
    });
});
