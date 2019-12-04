import messages from "./data/messages";
import plainTextPart from "./data/plainTextPart";
import MailboxItemsStore from "../../src/MailboxItemsStore";
import Message from "../../src/MailboxItemsStore/Message";
import ServiceLocator from "@bluemind/inject";
import { MailboxItemsClient } from "@bluemind/backend.mail.api";
import ItemUri from "@bluemind/item-uri";
import { createLocalVue } from "@vue/test-utils";
import Vuex from "vuex";
import cloneDeep from "lodash.clonedeep";

jest.mock("@bluemind/inject");
jest.mock("@bluemind/backend.mail.api");

const service = new MailboxItemsClient();
const get = jest.fn().mockReturnValue(service);
ServiceLocator.getProvider.mockReturnValue({
    get
});

const localVue = createLocalVue();
localVue.use(Vuex);

describe("[MailboxItemsStore] Vuex store", () => {
    test("can load messages from a folder into store", done => {
        const folderUid = "folder:uid";
        const sorted = { direction: "desc", column: "internal_date" };
        service.sortedIds.mockReturnValueOnce(Promise.resolve(messages.map(message => message.internalId)));
        service.multipleById.mockReturnValueOnce(Promise.resolve(messages));
        const store = new Vuex.Store(cloneDeep(MailboxItemsStore));

        store
            .dispatch("list", { sorted, folderUid })
            .then(() => {
                expect(store.state.itemKeys.length).toEqual(messages.length);
                return store.dispatch("multipleByKey", store.state.itemKeys);
            })
            .then(() => {
                messages.forEach((item, index) => {
                    const key = ItemUri.encode(item.internalId, folderUid);
                    expect(store.getters.indexOf(key)).toEqual(index);
                    const message = new Message(key, item);
                    expect(store.getters.getMessageByKey(key)).toEqual(message);
                });
                expect(store.getters.count).toEqual(messages.length);
                done();
            });
    });
    test("can remove a message", done => {
        const store = new Vuex.Store(cloneDeep(MailboxItemsStore));
        const folderUid = "folder:uid";
        store.commit("storeItems", { items: messages, folderUid });
        service.deleteById.mockReturnValueOnce(Promise.resolve());
        const message = messages[0];
        const key = ItemUri.encode(message.internalId, folderUid);
        store.dispatch("remove", key).then(() => {
            expect(store.state.itemKeys.includes(key)).not.toBeTruthy();
            expect(store.getters.indexOf(key)).toEqual(-1);
            expect(store.getters.getMessageByKey(key)).toBeUndefined();
            done();
        });
    });
    test("can mark a message as read", done => {
        const store = new Vuex.Store(cloneDeep(MailboxItemsStore));
        const folderUid = "folder:uid";
        store.commit("storeItems", { items: messages, folderUid });
        service.updateSeens.mockReturnValueOnce(Promise.resolve());
        const messageKey = ItemUri.encode(messages[0].internalId, folderUid);
        expect(messages[0].value.systemFlags.includes("seen")).toBeTruthy();
        store.dispatch("updateSeen", { messageKey, isSeen: false }).then(() => {
            const message = store.getters.getMessageByKey(messageKey);
            expect(message.states.includes("not-seen")).toBeTruthy();
            expect(message.flags.includes("seen")).not.toBeTruthy();
            done();
        });
    });
    test("can mark a message as unread", done => {
        const store = new Vuex.Store(cloneDeep(MailboxItemsStore));
        const folderUid = "folder:uid";
        store.commit("storeItems", { items: messages, folderUid });
        service.updateSeens.mockReturnValueOnce(Promise.resolve());
        const messageKey = ItemUri.encode(messages[5].internalId, folderUid);
        expect(messages[3].value.systemFlags.includes("seen")).not.toBeTruthy();
        store.dispatch("updateSeen", { messageKey, isSeen: true }).then(() => {
            const message = store.getters.getMessageByKey(messageKey);
            expect(message.states.includes("not-seen")).not.toBeTruthy();
            expect(message.flags.includes("seen")).toBeTruthy();
            done();
        });
    });
    test("can read a message part", done => {
        const store = new Vuex.Store(cloneDeep(MailboxItemsStore));
        const folderUid = "folder:uid";
        store.commit("storeItems", { items: messages, folderUid });
        service.fetch.mockReturnValue(Promise.resolve(plainTextPart));
        const messageKey = ItemUri.encode(messages[5].internalId, folderUid);
        const message = store.getters.getMessageByKey(messageKey);
        const inlines = message.computeParts().inlines[0];
        Promise.all(inlines.parts.map(part => store.dispatch("fetch", { messageKey, part, isAttachment: false }))).then(
            () => {
                const parts = store.state.itemsParts[messageKey];
                expect(parts.length).toEqual(1);
                expect(store.state.partContents[parts[0]]).toEqual(plainTextPart);
                done();
            }
        );
    });
});
