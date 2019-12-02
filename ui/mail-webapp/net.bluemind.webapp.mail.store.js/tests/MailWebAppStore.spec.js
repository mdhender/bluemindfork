import MailWebAppStore from "../src";
import aliceFolders from "./datas/alice/folders";
import aliceInbox from "./datas/alice/inbox";

import ServiceLocator from "@bluemind/inject";
import { MailboxFoldersClient, MailboxItemsClient } from "@bluemind/backend.mail.api";
// import ItemUri from "@bluemind/item-uri";
import { createLocalVue } from "@vue/test-utils";
import Vuex from "vuex";
import cloneDeep from "lodash.clonedeep";
import ItemUri from "@bluemind/item-uri";

jest.mock("@bluemind/inject");
jest.mock("@bluemind/backend.mail.api");

let foldersService;
let itemsService;
ServiceLocator.getProvider.mockImplementation(api =>
    api == "MailboxFoldersPersistence" ? { get: () => foldersService } : { get: () => itemsService }
);

const localVue = createLocalVue();
localVue.use(Vuex);

describe("[MailWebAppStore] Vuex store", () => {
    let store;
    beforeEach(() => {
        store = new Vuex.Store(cloneDeep(MailWebAppStore));
        MailboxFoldersClient.mockClear();
        MailboxItemsClient.mockClear();
        foldersService = new MailboxFoldersClient();
        itemsService = new MailboxItemsClient();
    });
    test("bootstrap load folders from my mailbox into store with unread count", done => {
        foldersService.all.mockReturnValueOnce(Promise.resolve(aliceFolders));
        itemsService.sortedIds.mockReturnValueOnce(Promise.resolve(aliceInbox.map(message => message.internalId)));
        itemsService.multipleById.mockReturnValueOnce(Promise.resolve(aliceInbox));
        itemsService.getPerUserUnread.mockReturnValue(Promise.resolve({ count: 0 }));
        itemsService.getPerUserUnread.mockReturnValueOnce(Promise.resolve({ count: 10 }));
        itemsService.getPerUserUnread.mockReturnValueOnce(Promise.resolve({ count: 15 }));
        store.dispatch("bootstrap", "alice@blue-mind.loc").then(() => {
            expect(store.getters.my.mailboxUid).toEqual("user.alice");
            expect(store.getters.my.INBOX.uid).toEqual("f1c3f42f-551b-446d-9682-cfe0574b3205");
            expect(store.getters.my.TRASH.uid).toEqual("98a9383e-5156-44eb-936a-e6b0825b0809");
            expect(store.getters.my.folders.length).toEqual(aliceFolders.length);
            expect(store.getters.my.folders).toEqual(store.getters["folders/getFoldersByMailbox"]("user.alice"));
            expect(store.getters.currentFolder).toBe(store.getters.my.INBOX);
            done();
        });
    });
    test("select a folder", done => {
        store.commit("folders/storeItems", { items: aliceFolders, mailboxUid: "user.alice" });
        const folderUid = "050ca560-37ae-458a-bd52-c40fedf4068d";
        const folderKey = ItemUri.encode(folderUid, "user.alice");
        itemsService.sortedIds.mockReturnValueOnce(Promise.resolve(aliceInbox.map(message => message.internalId)));
        itemsService.multipleById.mockImplementationOnce(ids =>
            Promise.resolve(aliceInbox.filter(message => ids.includes(message.internalId + "")))
        );
        store.dispatch("selectFolder", folderKey).then(() => {
            expect(store.state.currentFolderKey).toEqual(folderKey);
            expect(store.state.messages.itemKeys.length).toEqual(aliceInbox.length);
            expect(store.state.messages.itemKeys).toEqual(aliceInbox.map(m => ItemUri.encode(m.internalId, folderUid)));
            expect(store.state.messages.items[store.state.messages.itemKeys[0]]).not.toBeUndefined();

            done();
        });
    });
    test("select a message", done => {
        const folderUid = "f1c3f42f-551b-446d-9682-cfe0574b3205";
        const messageKey = ItemUri.encode(872, folderUid);
        itemsService.fetch.mockReturnValueOnce(Promise.resolve("Text content..."));
        itemsService.fetch.mockReturnValueOnce(Promise.resolve("Ruby content"));

        store.commit("messages/storeItems", { items: aliceInbox, folderUid });
        store.dispatch("selectMessage", messageKey).then(() => {
            expect(store.getters.currentMessage).toBe(store.getters["messages/getMessageByKey"](messageKey));
            let parts = [
                {
                    mime: "text/plain",
                    address: "1",
                    encoding: "quoted-printable",
                    charset: "ISO-8859-1",
                    size: 25,
                    content: "Text content..."
                }
            ];
            expect(store.getters.currentMessageContent).toEqual(parts);
            parts = [
                {
                    mime: "text/x-ruby-script",
                    address: "2",
                    encoding: "7bit",
                    charset: "us-ascii",
                    filename: "api.rb",
                    size: 28
                }
            ];
            expect(store.getters.currentMessageAttachments).toEqual(parts);

            done();
        });
    });
    test("remove a message", done => {
        const folderUid = "f1c3f42f-551b-446d-9682-cfe0574b3205";
        let messageKey = ItemUri.encode(872, folderUid);
        foldersService.importItems.mockReturnValueOnce(Promise.resolve());
        store.commit("setUserLogin", "alice@blue-mind.loc");
        store.commit("messages/storeItems", { items: aliceInbox, folderUid });
        store.commit("folders/storeItems", { items: aliceFolders, mailboxUid: "user.alice" });
        expect(store.state.messages.itemKeys).toEqual(expect.arrayContaining([messageKey]));

        store.dispatch("remove", messageKey).then(() => {
            expect(store.state.messages.itemKeys).toEqual(expect.not.arrayContaining([messageKey]));
            expect(foldersService.importItems).toHaveBeenCalled();
            done();
        });
    });
    test("remove a message from trash", done => {
        const trashUid = "98a9383e-5156-44eb-936a-e6b0825b0809";
        const messageKey = ItemUri.encode(872, trashUid);
        itemsService.deleteById.mockReturnValueOnce(Promise.resolve());
        store.commit("setUserLogin", "alice@blue-mind.loc");
        store.commit("messages/storeItems", { items: aliceInbox, folderUid: trashUid });
        store.commit("folders/storeItems", { items: aliceFolders, mailboxUid: "user.alice" });
        expect(store.state.messages.itemKeys).toEqual(expect.arrayContaining([messageKey]));

        store.dispatch("remove", messageKey).then(() => {
            expect(store.state.messages.itemKeys).toEqual(expect.not.arrayContaining([messageKey]));
            expect(itemsService.deleteById).toHaveBeenCalled();
            done();
        });
    });
    test("remove a message definitely", done => {
        const folderUid = "f1c3f42f-551b-446d-9682-cfe0574b3205";
        const messageKey = ItemUri.encode(872, folderUid);
        itemsService.deleteById.mockReturnValueOnce(Promise.resolve());
        store.commit("setUserLogin", "alice@blue-mind.loc");
        store.commit("messages/storeItems", { items: aliceInbox, folderUid });
        store.commit("folders/storeItems", { items: aliceFolders, mailboxUid: "user.alice" });
        expect(store.state.messages.itemKeys).toEqual(expect.arrayContaining([messageKey]));
        store.dispatch("purge", messageKey).then(() => {
            expect(store.state.messages.itemKeys).toEqual(expect.not.arrayContaining([messageKey]));
            expect(itemsService.deleteById).toHaveBeenCalled();
            done();
        });
    });
    test("move a message", done => {
        const inboxUid = "f1c3f42f-551b-446d-9682-cfe0574b3205";
        const archive2017Key = ItemUri.encode("050ca560-37ae-458a-bd52-c40fedf4068d", "user.alice");
        let messageKey = ItemUri.encode(872, inboxUid);
        foldersService.importItems.mockReturnValue(Promise.resolve());
        store.commit("setUserLogin", "alice@blue-mind.loc");
        store.commit("messages/storeItems", { items: aliceInbox, folderUid: inboxUid });
        store.commit("folders/storeItems", { items: aliceFolders, mailboxUid: "user.alice" });
        const destination = store.getters["folders/getFolderByKey"](archive2017Key);
        expect(store.state.messages.itemKeys).toEqual(expect.arrayContaining([messageKey]));
        store.dispatch("move", { messageKey, folder: destination }).then(() => {
            expect(store.state.messages.itemKeys).toEqual(expect.not.arrayContaining([messageKey]));
            expect(foldersService.importItems).toHaveBeenCalled();
            done();
        });
    });
    test("move a message in a new folder", done => {
        const inboxUid = "f1c3f42f-551b-446d-9682-cfe0574b3205";
        let messageKey = ItemUri.encode(872, inboxUid);
        let destination = {
            value: {
                name: "MyNewFolder",
                fullName: "MyNewFolder",
                parentUid: null,
                acls: [
                    { subject: "6793466E-F5D4-490F-97BF-DF09D3327BF4@blue-mind.loc", rights: "lrswipktecdan" },
                    { subject: "admin0", rights: "lrswipkxtecdan" }
                ]
            },
            uid: "210d2ead-d87c-4507-8219-5643106e035f",
            internalId: 2048,
            version: 1,
            displayName: "MyNewFolder",
            flags: []
        };
        let folderKey = ItemUri.encode(destination.uid, "user.alice");
        foldersService.importItems.mockReturnValue(Promise.resolve());
        foldersService.createBasic.mockReturnValue(Promise.resolve({ uid: destination.uid }));
        foldersService.getComplete.mockReturnValue(Promise.resolve(destination));
        store.commit("setUserLogin", "alice@blue-mind.loc");
        store.commit("messages/storeItems", { items: aliceInbox, folderUid: inboxUid });
        store.commit("folders/storeItems", { items: aliceFolders, mailboxUid: "user.alice" });

        store.dispatch("move", { messageKey, folder: destination }).then(() => {
            expect(store.state.messages.itemKeys).toEqual(expect.not.arrayContaining([messageKey]));
            expect(foldersService.importItems).toHaveBeenCalled();
            expect(foldersService.createBasic).toHaveBeenCalled();
            expect(store.state.folders.itemKeys).toEqual(expect.arrayContaining([folderKey]));
            done();
        });
    });
});
