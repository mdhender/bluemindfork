import Vuex from "vuex";
import { MockMailboxItemsClient } from "@bluemind/test-utils";
import ServiceLocator, { inject } from "@bluemind/inject";
import {
    alternativewithOneRelative,
    alternativewithTwoAttachments,
    alternativewithTwoAttachmentsOneInline,
    htmlContentWithInline,
    simpleAlternative,
    simpleDiv
} from "./data";
import cloneDeep from "lodash.clonedeep";
import { default as storeOptions } from "~/store/messages";

const key = "key";
const folderRef = { uid: "uid" };
let service, store, message;

describe("DEBOUNCED_SET_MESSAGE_CONTENT", () => {
    beforeEach(() => {
        ServiceLocator.register({ provide: "MailboxItemsPersistence", use: new MockMailboxItemsClient() });
        service = inject("MailboxItemsPersistence", "uid");
        store = new Vuex.Store(cloneDeep(storeOptions));
        message = {
            key,
            folderRef: folderRef,
            structure: null
        };
    });
    test("add new inline upload a new part", async () => {
        const htmlContent = htmlContentWithInline;
        message.structure = simpleAlternative;

        store.commit("ADD_MESSAGES", { messages: [message] });
        await store.dispatch("DEBOUNCED_SET_MESSAGE_CONTENT", { message, content: htmlContent });
        expect(service.uploadPart).toHaveBeenCalled();
    });
    test("set message preview", async () => {
        const content = "content";
        message.structure = simpleAlternative;
        store.commit("ADD_MESSAGES", { messages: [message] });
        expect(store.state[key].preview).toBeFalsy();

        await store.dispatch("DEBOUNCED_SET_MESSAGE_CONTENT", { message, content });
        expect(store.state[key].preview).toEqual(content);
    });
    test("add new inline image in structure without attachments", async () => {
        const htmlContent = htmlContentWithInline;
        message.structure = simpleAlternative;
        store.commit("ADD_MESSAGES", { messages: [message] });
        await store.dispatch("DEBOUNCED_SET_MESSAGE_CONTENT", { message, content: htmlContent });
        expect(store.state[message.key].structure).toEqual(alternativewithOneRelative);
    });
    test("add new inline image in structure with attachments", async () => {
        const htmlContent = htmlContentWithInline;
        message.structure = alternativewithTwoAttachments;
        store.commit("ADD_MESSAGES", { messages: [message] });
        await store.dispatch("DEBOUNCED_SET_MESSAGE_CONTENT", { message, content: htmlContent });
        expect(store.state[message.key].structure).toEqual(alternativewithTwoAttachmentsOneInline);
    });
    test("remove inline image in structure", async () => {
        const htmlContent = simpleDiv;
        message.structure = alternativewithOneRelative;
        store.commit("ADD_MESSAGES", { messages: [message] });
        await store.dispatch("DEBOUNCED_SET_MESSAGE_CONTENT", { message, content: htmlContent });
        expect(store.state[message.key].structure).toEqual(simpleAlternative);
    });
});
