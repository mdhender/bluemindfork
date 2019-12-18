import { createLocalVue } from "@vue/test-utils";
import { MockMailboxItemsClient } from "@bluemind/test-mocks";
import cloneDeep from "lodash.clonedeep";
import exampleMessages from "./data/messages";
import ItemUri from "@bluemind/item-uri";
import MailboxItemsStore from "../../src/MailboxItemsStore";
import Message from "../../src/MailboxItemsStore/Message";
import plainTextPart from "./data/plainTextPart";
import ServiceLocator from "@bluemind/inject";
import Vuex from "vuex";


const mockedClient = new MockMailboxItemsClient();
ServiceLocator.register({ provide: "MailboxItemsPersistence", factory: () => mockedClient });

const localVue = createLocalVue();
localVue.use(Vuex);

describe("[MailboxItemsStore] Vuex store", () => {
    test("can load messages from a folder into store", done => {
        const folderUid = "folder:uid";
        const sorted = { direction: "desc", column: "internal_date" };
        mockedClient.sortedIds.mockReturnValueOnce(Promise.resolve(exampleMessages.map(message => message.internalId)));
        mockedClient.multipleById.mockReturnValueOnce(Promise.resolve(exampleMessages));
        const store = new Vuex.Store(cloneDeep(MailboxItemsStore));

        store
            .dispatch("list", { sorted, folderUid })
            .then(() => {
                expect(store.state.itemKeys.length).toEqual(exampleMessages.length);
                return store.dispatch("multipleByKey", store.state.itemKeys);
            })
            .then(() => {
                exampleMessages.forEach((item, index) => {
                    const key = ItemUri.encode(item.internalId, folderUid);
                    expect(store.getters.indexOf(key)).toEqual(index);
                    const message = new Message(key, item);
                    expect(store.getters.getMessageByKey(key)).toEqual(message);
                });
                expect(store.getters.count).toEqual(exampleMessages.length);
                done();
            });
    });
    test("can remove a message", done => {
        const store = new Vuex.Store(cloneDeep(MailboxItemsStore));
        const folderUid = "folder:uid";
        store.commit("storeItems", { items: exampleMessages, folderUid });
        const message = exampleMessages[0];
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
        store.commit("storeItems", { items: exampleMessages, folderUid });
        const messageKey = ItemUri.encode(exampleMessages[0].internalId, folderUid);
        expect(exampleMessages[0].value.systemFlags.includes("seen")).toBeTruthy();
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
        store.commit("storeItems", { items: exampleMessages, folderUid });
        const messageKey = ItemUri.encode(exampleMessages[5].internalId, folderUid);
        expect(exampleMessages[3].value.systemFlags.includes("seen")).not.toBeTruthy();
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
        mockedClient.mockFetch(plainTextPart);
        store.commit("storeItems", { items: exampleMessages, folderUid });
        const messageKey = ItemUri.encode(exampleMessages[5].internalId, folderUid);
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
