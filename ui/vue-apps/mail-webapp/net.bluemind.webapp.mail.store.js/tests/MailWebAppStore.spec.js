import { ContainersClient } from "@bluemind/core.container.api";
import { MailboxesClient } from "@bluemind/mailbox.api";
import { UserSettingsClient } from "@bluemind/user.api";
import { createLocalVue } from "@vue/test-utils";
import { MockItemsTransferClient, MockMailboxItemsClient, MockMailboxFoldersClient } from "@bluemind/test-mocks";
import aliceFolders from "./data/alice/folders";
import aliceInbox from "./data/alice/inbox";
import cloneDeep from "lodash.clonedeep";
import containers from "./data/alice/containers";
import ItemUri from "@bluemind/item-uri";
import MailWebAppStore from "../src";
import readOnlyFolders from "./data/read.only/folders";
import ServiceLocator from "@bluemind/inject";
import sharedFolders from "./data/shared/folders";
import Vuex from "vuex";
import { Flag } from "@bluemind/email";

jest.mock("@bluemind/core.container.api");
jest.mock("@bluemind/mailbox.api");
jest.mock("@bluemind/user.api");

let containerService,
    mailboxesService,
    foldersService = new MockMailboxFoldersClient(),
    itemsService = new MockMailboxItemsClient(),
    itemsTransferClient = new MockItemsTransferClient(),
    userSettingsService;
ServiceLocator.register({ provide: "ContainersPersistence", factory: () => containerService });
ServiceLocator.register({ provide: "MailboxesPersistence", factory: () => mailboxesService });
ServiceLocator.register({ provide: "UserSettingsPersistence", factory: () => userSettingsService });
ServiceLocator.register({ provide: "MailboxItemsPersistence", factory: () => itemsService });
ServiceLocator.register({ provide: "MailboxFoldersPersistence", factory: () => foldersService });
ServiceLocator.register({ provide: "ItemsTransferPersistence", factory: () => itemsTransferClient });
ServiceLocator.register({ provide: "UserSession", factory: () => "" });

const localVue = createLocalVue();
localVue.use(Vuex);

describe("[MailWebAppStore] Vuex store", () => {
    let store;
    beforeEach(() => {
        store = new Vuex.Store();
        store.registerModule("mail-webapp", cloneDeep(MailWebAppStore));
        ContainersClient.mockClear();
        MailboxesClient.mockClear();
        foldersService = new MockMailboxFoldersClient();
        itemsService = new MockMailboxItemsClient();
        itemsTransferClient = new MockItemsTransferClient();
        containerService = new ContainersClient();
        mailboxesService = new MailboxesClient();
        userSettingsService = new UserSettingsClient();
    });
    test("bootstrap load folders into store with unread count", done => {
        let letsEndThis;
        const awaitForLastCall = new Promise(resolve => {
            letsEndThis = resolve;
        });
        foldersService.all.mockReturnValueOnce(Promise.resolve(aliceFolders));
        foldersService.all.mockReturnValueOnce(Promise.resolve(readOnlyFolders));
        foldersService.all.mockImplementationOnce(() => {
            letsEndThis();
            return Promise.resolve(sharedFolders);
        });
        itemsService.sortedIds.mockReturnValueOnce(Promise.resolve(aliceInbox.map(message => message.internalId)));
        itemsService.multipleById.mockReturnValueOnce(Promise.resolve(aliceInbox));
        itemsService.getPerUserUnread.mockReturnValue(Promise.resolve({ count: 0 }));
        itemsService.getPerUserUnread.mockReturnValueOnce(Promise.resolve({ count: 10 }));
        itemsService.getPerUserUnread.mockReturnValueOnce(Promise.resolve({ count: 15 }));
        containerService.all.mockReturnValueOnce(Promise.resolve(containers));
        mailboxesService.getMailboxConfig.mockReturnValue(Promise.resolve({}));
        const mockedMessageListStyle = "compact";
        userSettingsService.getOne.mockReturnValue(Promise.resolve(mockedMessageListStyle));

        store
            .dispatch("mail-webapp/bootstrap", "alice@blue-mind.loc", { root: true })
            .then(() => {
                expect(store.state["mail-webapp"].login).toBe("alice@blue-mind.loc");
                expect(store.state["mail-webapp"].userSettings).toMatchObject({
                    mail_message_list_style: mockedMessageListStyle
                });
                expect(store.getters["mail-webapp/my"].mailboxUid).toEqual("user.alice");
                expect(store.getters["mail-webapp/my"].INBOX.uid).toEqual("f1c3f42f-551b-446d-9682-cfe0574b3205");
                expect(store.getters["mail-webapp/my"].TRASH.uid).toEqual("98a9383e-5156-44eb-936a-e6b0825b0809");
                expect(store.getters["mail-webapp/my"].folders.length).toEqual(aliceFolders.length);
                expect(store.getters["mail-webapp/my"].folders).toEqual(
                    store.getters["mail-webapp/folders/getFoldersByMailbox"]("user.alice")
                );
                expect(store.getters["mail-webapp/currentFolder"]).toBe(store.getters["mail-webapp/my"].INBOX);

                return awaitForLastCall;
            })
            .then(() => {
                expect(store.getters["mail-webapp/mailshares"].length).toBe(2);
                expect(store.getters["mail-webapp/mailshares"][0].folders.length).toBe(2);
                expect(store.getters["mail-webapp/mailshares"][0].folders[0].uid).toEqual(
                    "8933808e-7ce2-45c6-9fe6-e003bf11eb2e"
                );
                done();
            });
    });
    test("select a folder", done => {
        store.commit(
            "mail-webapp/folders/storeItems",
            { items: aliceFolders, mailboxUid: "user.alice" },
            { root: true }
        );
        store.commit("mail-webapp/setUserLogin", "alice@blue-mind.loc", { root: true });

        const folderUid = "050ca560-37ae-458a-bd52-c40fedf4068d";
        const folderKey = ItemUri.encode(folderUid, "user.alice");
        itemsService.sortedIds.mockReturnValueOnce(Promise.resolve(aliceInbox.map(message => message.internalId)));
        itemsService.getPerUserUnread.mockReturnValueOnce(Promise.resolve({ total: 4 }));
        itemsService.multipleById.mockImplementationOnce(ids =>
            Promise.resolve(aliceInbox.filter(message => ids.includes(message.internalId)))
        );
        store.dispatch("mail-webapp/loadMessageList", { folder: folderKey }, { root: true }).then(() => {
            expect(store.state["mail-webapp"].currentFolderKey).toEqual(folderKey);
            expect(store.state["mail-webapp"].messages.itemKeys.length).toEqual(aliceInbox.length);
            expect(store.state["mail-webapp"].messages.itemKeys).toEqual(
                aliceInbox.map(m => ItemUri.encode(m.internalId, folderUid))
            );
            expect(
                store.state["mail-webapp"].messages.items[store.state["mail-webapp"].messages.itemKeys[0]]
            ).not.toBeUndefined();

            done();
        });
    });
    test("select a folder with 'unread' filter", done => {
        store.commit("mail-webapp/setUserLogin", "alice@blue-mind.loc", { root: true });
        store.commit(
            "mail-webapp/folders/storeItems",
            { items: aliceFolders, mailboxUid: "user.alice" },
            { root: true }
        );
        const folderUid = "050ca560-37ae-458a-bd52-c40fedf4068d";
        const folderKey = ItemUri.encode(folderUid, "user.alice");
        itemsService.getPerUserUnread.mockReturnValue(Promise.resolve({ total: 5 }));
        itemsService.filteredChangesetById.mockReturnValue(
            Promise.resolve({
                created: aliceInbox
                    .filter(message => !message.value.flags.includes(Flag.SEEN))
                    .map(message => {
                        return { id: message.internalId };
                    })
            })
        );
        itemsService.multipleById.mockImplementation(() =>
            Promise.resolve(aliceInbox.filter(message => !message.value.flags.includes(Flag.SEEN)))
        );

        store
            .dispatch("mail-webapp/loadMessageList", { folder: folderKey, filter: "unread" }, { root: true })
            .then(() => {
                expect(store.state["mail-webapp"].currentFolderKey).toEqual(folderKey);
                expect(store.state["mail-webapp"].messages.itemKeys.length).toEqual(5);
                expect(store.state["mail-webapp"].messages.itemKeys).toEqual(
                    aliceInbox
                        .filter(message => !message.flags.includes("Seen"))
                        .map(m => ItemUri.encode(m.internalId, folderUid))
                );
                expect(
                    store.state["mail-webapp"].messages.items[store.state["mail-webapp"].messages.itemKeys[0]]
                ).not.toBeUndefined();
                expect(store.state["mail-webapp"].foldersData[folderUid].unread).toBe(5);
                expect(store.getters["mail-webapp/currentMailbox"].mailboxUid).toEqual("user.alice");
                done();
            });
    });
    test("select a message", done => {
        const folderUid = "f1c3f42f-551b-446d-9682-cfe0574b3205";
        const messageKey = ItemUri.encode(872, folderUid);
        const text = "Text content...";
        itemsService.mockFetch(text);

        store.commit("mail-webapp/messages/storeItems", { items: aliceInbox, folderUid }, { root: true });
        store.dispatch("mail-webapp/selectMessage", messageKey, { root: true }).then(() => {
            expect(store.getters["mail-webapp/currentMessage/message"]).toStrictEqual(
                store.getters["mail-webapp/messages/getMessagesByKey"]([messageKey])[0]
            );
            let parts = [
                {
                    mime: "text/plain",
                    address: "1",
                    encoding: "quoted-printable",
                    charset: "ISO-8859-1",
                    size: 25,
                    content: text
                }
            ];
            expect(store.getters["mail-webapp/currentMessage/content"]).toEqual(parts);
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
            expect(store.state["mail-webapp"].currentMessage.parts.attachments).toEqual(parts);

            done();
        });
    });
    test("remove a message from my mailbox", done => {
        const folderUid = "f1c3f42f-551b-446d-9682-cfe0574b3205";
        const folderKey = ItemUri.encode(folderUid, "user.alice");
        let messageKey = ItemUri.encode(872, folderUid);
        store.commit("mail-webapp/setUserLogin", "alice@blue-mind.loc", { root: true });
        store.commit("mail-webapp/messages/storeItems", { items: aliceInbox, folderUid }, { root: true });
        store.commit(
            "mail-webapp/folders/storeItems",
            { items: aliceFolders, mailboxUid: "user.alice" },
            { root: true }
        );
        store.commit("mail-webapp/setCurrentFolder", folderKey, { root: true });
        expect(store.state["mail-webapp"].messages.itemKeys).toEqual(expect.arrayContaining([messageKey]));

        store.dispatch("mail-webapp/remove", messageKey, { root: true }).then(() => {
            expect(store.state["mail-webapp"].messages.itemKeys).toEqual(expect.not.arrayContaining([messageKey]));
            expect(foldersService.importItems).toHaveBeenCalled();
            done();
        });
    });
    test("remove a message from a mailshare", done => {
        const folderUid = "f1c3f42f-551b-446d-9682-cfe0574b3205";
        const folderKey = ItemUri.encode(folderUid, "2814CC5D-D372-4F66-A434-89863E99B8CD");
        let messageKey = ItemUri.encode(872, folderUid);
        itemsService.fetchComplete.mockReturnValueOnce(Promise.resolve("eml"));
        itemsService.uploadPart.mockReturnValueOnce(Promise.resolve("addr"));
        store.commit("mail-webapp/setUserLogin", "alice@blue-mind.loc", { root: true });
        store.commit("mail-webapp/mailboxes/storeContainers", containers, { root: true });
        store.commit(
            "mail-webapp/folders/storeItems",
            { items: aliceFolders, mailboxUid: "user.alice" },
            { root: true }
        );
        store.commit(
            "mail-webapp/folders/storeItems",
            {
                items: sharedFolders,
                mailboxUid: "2814CC5D-D372-4F66-A434-89863E99B8CD"
            },
            { root: true }
        );
        store.commit("mail-webapp/messages/storeItems", { items: aliceInbox, folderUid }, { root: true });
        store.commit("mail-webapp/setCurrentFolder", folderKey, { root: true });
        expect(store.state["mail-webapp"].messages.itemKeys).toEqual(expect.arrayContaining([messageKey]));
        store.dispatch("mail-webapp/remove", messageKey, { root: true }).then(() => {
            expect(store.state["mail-webapp"].messages.itemKeys).toEqual(expect.not.arrayContaining([messageKey]));
            expect(itemsTransferClient.move).toHaveBeenCalled();
            done();
        });
    });
    test("remove a message from trash", done => {
        const trashUid = "98a9383e-5156-44eb-936a-e6b0825b0809";
        const messageKey = ItemUri.encode(872, trashUid);
        store.commit("mail-webapp/setUserLogin", "alice@blue-mind.loc", { root: true });
        store.commit("mail-webapp/messages/storeItems", { items: aliceInbox, folderUid: trashUid }, { root: true });
        store.commit(
            "mail-webapp/folders/storeItems",
            { items: aliceFolders, mailboxUid: "user.alice" },
            { root: true }
        );
        expect(store.state["mail-webapp"].messages.itemKeys).toEqual(expect.arrayContaining([messageKey]));
        store.dispatch("mail-webapp/remove", messageKey, { root: true }).then(() => {
            expect(store.state["mail-webapp"].messages.itemKeys).toEqual(expect.not.arrayContaining([messageKey]));
            expect(itemsService.deleteById).toHaveBeenCalled();
            done();
        });
    });
    test("remove a message definitely", done => {
        const folderUid = "f1c3f42f-551b-446d-9682-cfe0574b3205";
        const messageKey = ItemUri.encode(872, folderUid);
        store.commit("mail-webapp/setUserLogin", "alice@blue-mind.loc", { root: true });
        store.commit("mail-webapp/messages/storeItems", { items: aliceInbox, folderUid }, { root: true });
        store.commit(
            "mail-webapp/folders/storeItems",
            { items: aliceFolders, mailboxUid: "user.alice" },
            { root: true }
        );
        expect(store.state["mail-webapp"].messages.itemKeys).toEqual(expect.arrayContaining([messageKey]));
        store.dispatch("mail-webapp/purge", messageKey, { root: true }).then(() => {
            expect(store.state["mail-webapp"].messages.itemKeys).toEqual(expect.not.arrayContaining([messageKey]));
            expect(itemsService.deleteById).toHaveBeenCalled();
            done();
        });
    });
    test("move a message", done => {
        const inboxUid = "f1c3f42f-551b-446d-9682-cfe0574b3205";
        const archive2017Key = ItemUri.encode("050ca560-37ae-458a-bd52-c40fedf4068d", "user.alice");
        const folderKey = ItemUri.encode(inboxUid, "user.alice");
        let messageKey = ItemUri.encode(872, inboxUid);
        store.commit("mail-webapp/setUserLogin", "alice@blue-mind.loc", { root: true });
        store.commit("mail-webapp/setCurrentFolder", folderKey, { root: true });
        store.commit("mail-webapp/messages/storeItems", { items: aliceInbox, folderUid: inboxUid }, { root: true });
        store.commit(
            "mail-webapp/folders/storeItems",
            { items: aliceFolders, mailboxUid: "user.alice" },
            { root: true }
        );
        const destination = store.getters["mail-webapp/folders/getFolderByKey"](archive2017Key);
        expect(store.state["mail-webapp"].messages.itemKeys).toEqual(expect.arrayContaining([messageKey]));
        store.dispatch("mail-webapp/move", { messageKey, folder: destination }, { root: true }).then(() => {
            expect(store.state["mail-webapp"].messages.itemKeys).toEqual(expect.not.arrayContaining([messageKey]));
            expect(foldersService.importItems).toHaveBeenCalled();
            done();
        });
    });
    test("move a message across mailboxes", done => {
        const folderUid = "f1c3f42f-551b-446d-9682-cfe0574b3205";
        const archive2017Key = ItemUri.encode("050ca560-37ae-458a-bd52-c40fedf4068d", "user.alice");
        const folderKey = ItemUri.encode(folderUid, "2814CC5D-D372-4F66-A434-89863E99B8CD");
        let messageKey = ItemUri.encode(872, folderUid);

        itemsService.fetchComplete.mockReturnValueOnce(Promise.resolve("eml"));
        itemsService.uploadPart.mockReturnValueOnce(Promise.resolve("addr"));

        store.commit("mail-webapp/setUserLogin", "alice@blue-mind.loc", { root: true });
        store.commit("mail-webapp/mailboxes/storeContainers", containers, { root: true });
        store.commit("mail-webapp/setCurrentFolder", folderKey, { root: true });
        store.commit("mail-webapp/messages/storeItems", { items: aliceInbox, folderUid }, { root: true });
        store.commit(
            "mail-webapp/folders/storeItems",
            { items: aliceFolders, mailboxUid: "user.alice" },
            { root: true }
        );
        store.commit(
            "mail-webapp/folders/storeItems",
            {
                items: sharedFolders,
                mailboxUid: "2814CC5D-D372-4F66-A434-89863E99B8CD"
            },
            { root: true }
        );
        const destination = store.getters["mail-webapp/folders/getFolderByKey"](archive2017Key);
        expect(store.state["mail-webapp"].messages.itemKeys).toEqual(expect.arrayContaining([messageKey]));
        store.dispatch("mail-webapp/move", { messageKey, folder: destination }, { root: true }).then(() => {
            expect(itemsTransferClient.move).toHaveBeenCalled();
            expect(store.state["mail-webapp"].messages.itemKeys).toEqual(expect.not.arrayContaining([messageKey]));
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
        foldersService.createBasic.mockReturnValue(Promise.resolve({ uid: destination.uid }));
        foldersService.getComplete.mockReturnValue(Promise.resolve(destination));
        store.commit("mail-webapp/setUserLogin", "alice@blue-mind.loc", { root: true });
        store.commit("mail-webapp/messages/storeItems", { items: aliceInbox, folderUid: inboxUid }, { root: true });
        store.commit(
            "mail-webapp/folders/storeItems",
            { items: aliceFolders, mailboxUid: "user.alice" },
            { root: true }
        );
        store.commit("mail-webapp/setCurrentFolder", folderKey, { root: true });

        store.dispatch("mail-webapp/move", { messageKey, folder: destination }, { root: true }).then(() => {
            expect(store.state["mail-webapp"].messages.itemKeys).toEqual(expect.not.arrayContaining([messageKey]));
            expect(foldersService.importItems).toHaveBeenCalled();
            expect(foldersService.createBasic).toHaveBeenCalled();
            expect(store.state["mail-webapp"].folders.itemKeys).toEqual(expect.arrayContaining([folderKey]));
            done();
        });
    });
});
