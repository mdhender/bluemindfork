import { MailboxesClient } from "@bluemind/mailbox.api";
import { UserSettingsClient } from "@bluemind/user.api";
import { createLocalVue } from "@vue/test-utils";
import {
    MockItemsTransferClient,
    MockMailboxItemsClient,
    MockMailboxFoldersClient,
    MockOwnerSubscriptionsClient,
    MockContainersClient
} from "@bluemind/test-mocks";
import aliceFolders from "./data/alice/folders";
import aliceInbox from "./data/alice/inbox";
import cloneDeep from "lodash.clonedeep";
import containers from "./data/alice/containers";
import ItemUri from "@bluemind/item-uri";
import MailWebAppStore from "../src";
import MailStore, { FETCH_MAILBOXES, FETCH_FOLDERS } from "@bluemind/webapp.mail.store";
import readOnlyFolders from "./data/read.only/folders";
import ServiceLocator from "@bluemind/inject";
import sharedFolders from "./data/shared/folders";
import Vuex from "vuex";
import WebsocketClient from "@bluemind/sockjs";
import { Flag } from "@bluemind/email";

jest.mock("@bluemind/sockjs");
jest.mock("@bluemind/mailbox.api");
jest.mock("@bluemind/user.api");

let containerService = new MockContainersClient(),
    mailboxesService,
    foldersService = new MockMailboxFoldersClient(),
    itemsService = new MockMailboxItemsClient(),
    itemsTransferClient = new MockItemsTransferClient(),
    userSettingsService;
ServiceLocator.register({ provide: "ContainersPersistence", factory: () => containerService });
ServiceLocator.register({ provide: "SubscriptionPersistence", factory: () => new MockOwnerSubscriptionsClient() });
ServiceLocator.register({ provide: "MailboxesPersistence", factory: () => mailboxesService });
ServiceLocator.register({ provide: "UserSettingsPersistence", factory: () => userSettingsService });
ServiceLocator.register({ provide: "MailboxItemsPersistence", factory: () => itemsService });
ServiceLocator.register({ provide: "MailboxFoldersPersistence", factory: () => foldersService });
ServiceLocator.register({ provide: "ItemsTransferPersistence", factory: () => itemsTransferClient });
ServiceLocator.register({
    provide: "UserSession",
    factory: () => {
        return {
            roles: ["hasCalendar"]
        };
    }
});

WebsocketClient.register = jest.fn();

const localVue = createLocalVue();
localVue.use(Vuex);

describe("[MailWebAppStore] Vuex store", () => {
    let store;
    beforeEach(() => {
        store = new Vuex.Store();
        store.registerModule("mail-webapp", cloneDeep(MailWebAppStore));
        store.registerModule("mail", cloneDeep(MailStore));
        MailboxesClient.mockClear();
        foldersService = new MockMailboxFoldersClient();
        itemsService = new MockMailboxItemsClient();
        itemsTransferClient = new MockItemsTransferClient();
        containerService = new MockContainersClient();
        mailboxesService = new MailboxesClient();
        userSettingsService = new UserSettingsClient();
    });
    test("bootstrap load folders into store with unread count", async () => {
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
        containerService.getContainers.mockReturnValueOnce(Promise.resolve(containers));
        mailboxesService.getMailboxConfig.mockReturnValue(Promise.resolve({}));
        const mockedMessageListStyle = "compact";
        userSettingsService.getOne.mockReturnValue(Promise.resolve(mockedMessageListStyle));

        await store.dispatch("mail-webapp/bootstrap", "6793466E-F5D4-490F-97BF-DF09D3327BF4", { root: true });
        expect(store.state["mail-webapp"].userUid).toBe("6793466E-F5D4-490F-97BF-DF09D3327BF4");
        expect(store.state["mail-webapp"].userSettings).toMatchObject({
            mail_message_list_style: mockedMessageListStyle
        });
        expect(store.getters["mail-webapp/my"].mailboxUid).toEqual("user.6793466E-F5D4-490F-97BF-DF09D3327BF4");
        expect(store.getters["mail-webapp/my"].INBOX.uid).toEqual("f1c3f42f-551b-446d-9682-cfe0574b3205");
        expect(store.getters["mail-webapp/my"].TRASH.uid).toEqual("98a9383e-5156-44eb-936a-e6b0825b0809");
        expect(store.getters["mail-webapp/my"].folders.length).toEqual(aliceFolders.length);
        expect(store.getters["mail-webapp/my"].folders).toEqual(
            store.getters["mail-webapp/folders/getFoldersByMailbox"]("user.6793466E-F5D4-490F-97BF-DF09D3327BF4")
        );

        await awaitForLastCall;
        expect(store.getters["mail-webapp/mailshares"].length).toBe(2);
        expect(store.getters["mail-webapp/mailshares"][0].folders.length).toBe(2);
        expect(store.getters["mail-webapp/mailshares"][0].folders[0].uid).toEqual(
            "97e386cc-caff-4931-8bd7-d921bd900f37"
        );
    });
    test("select a folder", async () => {
        //load data in new store
        containerService.getContainers.mockReturnValueOnce(Promise.resolve(containers));
        await store.dispatch(FETCH_MAILBOXES);
        foldersService.all.mockReturnValueOnce(Promise.resolve(aliceFolders));
        await store.dispatch(FETCH_FOLDERS, store.state.mail.mailboxes["user.6793466E-F5D4-490F-97BF-DF09D3327BF4"]);
        store.commit("mail-webapp/setUserUid", "6793466E-F5D4-490F-97BF-DF09D3327BF4", { root: true });

        const folderUid = "050ca560-37ae-458a-bd52-c40fedf4068d";
        const folderKey = ItemUri.encode(folderUid, "user.6793466E-F5D4-490F-97BF-DF09D3327BF4");
        itemsService.sortedIds.mockReturnValueOnce(Promise.resolve(aliceInbox.map(message => message.internalId)));
        itemsService.getPerUserUnread.mockReturnValueOnce(Promise.resolve({ total: 4 }));
        itemsService.multipleById.mockImplementationOnce(ids =>
            Promise.resolve(aliceInbox.filter(message => ids.includes(message.internalId)))
        );
        await store.dispatch("mail-webapp/loadMessageList", { folder: folderKey }, { root: true });
        expect(store.state["mail-webapp"].currentFolderKey).toEqual(folderKey);
        expect(store.state["mail-webapp"].messages.itemKeys.length).toEqual(aliceInbox.length);
        expect(store.state["mail-webapp"].messages.itemKeys).toEqual(
            aliceInbox.map(m => ItemUri.encode(m.internalId, folderUid))
        );
        expect(
            store.state["mail-webapp"].messages.items[store.state["mail-webapp"].messages.itemKeys[0]]
        ).not.toBeUndefined();
    });
    test("select a folder with 'unread' filter", async () => {
        //load data in new store
        containerService.getContainers.mockReturnValueOnce(Promise.resolve(containers));
        await store.dispatch(FETCH_MAILBOXES);
        foldersService.all.mockReturnValueOnce(Promise.resolve(aliceFolders));
        await store.dispatch(FETCH_FOLDERS, store.state.mail.mailboxes["user.6793466E-F5D4-490F-97BF-DF09D3327BF4"]);

        store.commit("mail-webapp/setUserUid", "6793466E-F5D4-490F-97BF-DF09D3327BF4", { root: true });

        const folderUid = "050ca560-37ae-458a-bd52-c40fedf4068d";
        const folderKey = ItemUri.encode(folderUid, "user.6793466E-F5D4-490F-97BF-DF09D3327BF4");
        itemsService.getPerUserUnread.mockReturnValue(Promise.resolve({ total: 5 }));
        itemsService.unreadItems.mockReturnValue(
            Promise.resolve(
                aliceInbox
                    .filter(message => !message.value.flags.includes(Flag.SEEN))
                    .map(message => message.internalId)
            )
        );
        itemsService.multipleById.mockImplementation(() =>
            Promise.resolve(aliceInbox.filter(message => !message.value.flags.includes(Flag.SEEN)))
        );

        expect(store.state["mail-webapp"].messages.itemKeys.length).toEqual(0);
        await store.dispatch("mail-webapp/loadMessageList", { folder: folderKey, filter: "unread" }, { root: true });
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
        expect(store.getters["mail-webapp/currentMailbox"].mailboxUid).toEqual(
            "user.6793466E-F5D4-490F-97BF-DF09D3327BF4"
        );
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
    test("remove a message from my mailbox", async () => {
        //load data in new store
        containerService.getContainers.mockReturnValueOnce(Promise.resolve(containers));
        await store.dispatch(FETCH_MAILBOXES);
        foldersService.all.mockReturnValueOnce(Promise.resolve(aliceFolders));
        await store.dispatch(FETCH_FOLDERS, store.state.mail.mailboxes["user.6793466E-F5D4-490F-97BF-DF09D3327BF4"]);

        const folderUid = "f1c3f42f-551b-446d-9682-cfe0574b3205";
        const folderKey = ItemUri.encode(folderUid, "user.6793466E-F5D4-490F-97BF-DF09D3327BF4");
        let messageKey = ItemUri.encode(872, folderUid);
        store.commit("mail-webapp/setUserUid", "6793466E-F5D4-490F-97BF-DF09D3327BF4", { root: true });
        store.commit("mail-webapp/messages/storeItems", { items: aliceInbox, folderUid }, { root: true });

        store.commit("mail-webapp/setCurrentFolder", folderKey, { root: true });
        expect(store.state["mail-webapp"].messages.itemKeys).toEqual(expect.arrayContaining([messageKey]));

        await store.dispatch("mail-webapp/remove", messageKey, { root: true });

        expect(store.state["mail-webapp"].messages.itemKeys).toEqual(expect.not.arrayContaining([messageKey]));
        expect(foldersService.importItems).toHaveBeenCalled();
    });
    test("remove a message from a mailshare", async () => {
        //load data
        containerService.getContainers.mockReturnValueOnce(Promise.resolve(containers));
        await store.dispatch(FETCH_MAILBOXES);
        foldersService.all.mockReturnValueOnce(Promise.resolve(aliceFolders));
        await store.dispatch(FETCH_FOLDERS, store.state.mail.mailboxes["user.6793466E-F5D4-490F-97BF-DF09D3327BF4"]);
        foldersService.all.mockReturnValueOnce(Promise.resolve(sharedFolders));
        await store.dispatch(FETCH_FOLDERS, store.state.mail.mailboxes["2814CC5D-D372-4F66-A434-89863E99B8CD"]);

        const folderUid = "f1c3f42f-551b-446d-9682-cfe0574b3205";
        const folderKey = ItemUri.encode(folderUid, "2814CC5D-D372-4F66-A434-89863E99B8CD");
        let messageKey = ItemUri.encode(872, folderUid);
        itemsService.fetchComplete.mockReturnValueOnce(Promise.resolve("eml"));
        itemsService.uploadPart.mockReturnValueOnce(Promise.resolve("addr"));
        store.commit("mail-webapp/setUserUid", "6793466E-F5D4-490F-97BF-DF09D3327BF4", { root: true });
        store.commit("mail-webapp/mailboxes/storeContainers", containers, { root: true });

        store.commit("mail-webapp/messages/storeItems", { items: aliceInbox, folderUid }, { root: true });
        store.commit("mail-webapp/setCurrentFolder", folderKey, { root: true });
        expect(store.state["mail-webapp"].messages.itemKeys).toEqual(expect.arrayContaining([messageKey]));
        await store.dispatch("mail-webapp/remove", messageKey, { root: true });
        expect(store.state["mail-webapp"].messages.itemKeys).toEqual(expect.not.arrayContaining([messageKey]));
        expect(itemsTransferClient.move).toHaveBeenCalled();
    });
    test("remove a message from trash", async () => {
        //load data in new store
        containerService.getContainers.mockReturnValueOnce(Promise.resolve(containers));
        await store.dispatch(FETCH_MAILBOXES);
        foldersService.all.mockReturnValueOnce(Promise.resolve(aliceFolders));
        await store.dispatch(FETCH_FOLDERS, store.state.mail.mailboxes["user.6793466E-F5D4-490F-97BF-DF09D3327BF4"]);

        const trashUid = "98a9383e-5156-44eb-936a-e6b0825b0809";
        const messageKey = ItemUri.encode(872, trashUid);
        store.commit("mail-webapp/setUserUid", "6793466E-F5D4-490F-97BF-DF09D3327BF4", { root: true });
        store.commit("mail-webapp/messages/storeItems", { items: aliceInbox, folderUid: trashUid }, { root: true });
        expect(store.state["mail-webapp"].messages.itemKeys).toEqual(expect.arrayContaining([messageKey]));
        await store.dispatch("mail-webapp/remove", messageKey, { root: true });
        expect(store.state["mail-webapp"].messages.itemKeys).toEqual(expect.not.arrayContaining([messageKey]));
        expect(itemsService.multipleDeleteById).toHaveBeenCalled();
    });
    test("remove a message definitely", done => {
        const folderUid = "f1c3f42f-551b-446d-9682-cfe0574b3205";
        const messageKey = ItemUri.encode(872, folderUid);
        store.commit("mail-webapp/setUserUid", "6793466E-F5D4-490F-97BF-DF09D3327BF4", { root: true });
        store.commit("mail-webapp/messages/storeItems", { items: aliceInbox, folderUid }, { root: true });
        store.commit(
            "mail-webapp/folders/storeItems",
            { items: aliceFolders, mailboxUid: "user.6793466E-F5D4-490F-97BF-DF09D3327BF4" },
            { root: true }
        );
        expect(store.state["mail-webapp"].messages.itemKeys).toEqual(expect.arrayContaining([messageKey]));
        store.dispatch("mail-webapp/purge", messageKey, { root: true }).then(() => {
            expect(store.state["mail-webapp"].messages.itemKeys).toEqual(expect.not.arrayContaining([messageKey]));
            expect(itemsService.multipleDeleteById).toHaveBeenCalled();
            done();
        });
    });
    test("move a message", async () => {
        //load data in new store
        containerService.getContainers.mockReturnValueOnce(Promise.resolve(containers));
        await store.dispatch(FETCH_MAILBOXES);
        foldersService.all.mockReturnValueOnce(Promise.resolve(aliceFolders));
        await store.dispatch(FETCH_FOLDERS, store.state.mail.mailboxes["user.6793466E-F5D4-490F-97BF-DF09D3327BF4"]);

        const inboxUid = "f1c3f42f-551b-446d-9682-cfe0574b3205";
        const archive2017Key = ItemUri.encode(
            "050ca560-37ae-458a-bd52-c40fedf4068d",
            "user.6793466E-F5D4-490F-97BF-DF09D3327BF4"
        );
        const folderKey = ItemUri.encode(inboxUid, "user.6793466E-F5D4-490F-97BF-DF09D3327BF4");
        let messageKey = ItemUri.encode(872, inboxUid);
        store.commit("mail-webapp/setUserUid", "6793466E-F5D4-490F-97BF-DF09D3327BF4", { root: true });
        store.commit("mail-webapp/setCurrentFolder", folderKey, { root: true });
        store.commit("mail-webapp/messages/storeItems", { items: aliceInbox, folderUid: inboxUid }, { root: true });
        const destination = store.getters["mail-webapp/folders/getFolderByKey"](archive2017Key);
        expect(store.state["mail-webapp"].messages.itemKeys).toEqual(expect.arrayContaining([messageKey]));
        await store.dispatch("mail-webapp/move", { messageKey, folder: destination }, { root: true });
        expect(store.state["mail-webapp"].messages.itemKeys).toEqual(expect.not.arrayContaining([messageKey]));
        expect(foldersService.importItems).toHaveBeenCalled();
    });
    test("move a message across mailboxes", async () => {
        //load data in new store
        containerService.getContainers.mockReturnValueOnce(Promise.resolve(containers));
        await store.dispatch(FETCH_MAILBOXES);
        foldersService.all.mockReturnValueOnce(Promise.resolve(aliceFolders));
        await store.dispatch(FETCH_FOLDERS, store.state.mail.mailboxes["user.6793466E-F5D4-490F-97BF-DF09D3327BF4"]);
        foldersService.all.mockReturnValueOnce(Promise.resolve(sharedFolders));
        await store.dispatch(FETCH_FOLDERS, store.state.mail.mailboxes["2814CC5D-D372-4F66-A434-89863E99B8CD"]);

        const folderUid = "f1c3f42f-551b-446d-9682-cfe0574b3205";
        const archive2017Key = ItemUri.encode(
            "050ca560-37ae-458a-bd52-c40fedf4068d",
            "user.6793466E-F5D4-490F-97BF-DF09D3327BF4"
        );
        const folderKey = ItemUri.encode(folderUid, "2814CC5D-D372-4F66-A434-89863E99B8CD");
        let messageKey = ItemUri.encode(872, folderUid);

        itemsService.fetchComplete.mockReturnValueOnce(Promise.resolve("eml"));
        itemsService.uploadPart.mockReturnValueOnce(Promise.resolve("addr"));

        store.commit("mail-webapp/setUserUid", "6793466E-F5D4-490F-97BF-DF09D3327BF4", { root: true });
        store.commit("mail-webapp/mailboxes/storeContainers", containers, { root: true });
        store.commit("mail-webapp/setCurrentFolder", folderKey, { root: true });
        store.commit("mail-webapp/messages/storeItems", { items: aliceInbox, folderUid }, { root: true });
        const destination = store.getters["mail-webapp/folders/getFolderByKey"](archive2017Key);
        expect(store.state["mail-webapp"].messages.itemKeys).toEqual(expect.arrayContaining([messageKey]));
        await store.dispatch("mail-webapp/move", { messageKey, folder: destination }, { root: true });
        expect(itemsTransferClient.move).toHaveBeenCalled();
        expect(store.state["mail-webapp"].messages.itemKeys).toEqual(expect.not.arrayContaining([messageKey]));
    });
    test("move a message in a new folder", async () => {
        //load data in new store
        containerService.getContainers.mockReturnValueOnce(Promise.resolve(containers));
        await store.dispatch(FETCH_MAILBOXES);
        foldersService.all.mockReturnValueOnce(Promise.resolve(aliceFolders));
        await store.dispatch(FETCH_FOLDERS, store.state.mail.mailboxes["user.6793466E-F5D4-490F-97BF-DF09D3327BF4"]);

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
        let folderKey = ItemUri.encode(destination.uid, "user.6793466E-F5D4-490F-97BF-DF09D3327BF4");
        foldersService.createBasic.mockReturnValue(Promise.resolve({ uid: destination.uid }));
        foldersService.getComplete.mockReturnValue(Promise.resolve(destination));
        store.commit("mail-webapp/setUserUid", "6793466E-F5D4-490F-97BF-DF09D3327BF4", { root: true });
        store.commit("mail-webapp/messages/storeItems", { items: aliceInbox, folderUid: inboxUid }, { root: true });
        store.commit("mail-webapp/setCurrentFolder", folderKey, { root: true });

        await store.dispatch("mail-webapp/move", { messageKey, folder: destination }, { root: true });

        expect(store.state["mail-webapp"].messages.itemKeys).toEqual(expect.not.arrayContaining([messageKey]));
        expect(foldersService.importItems).toHaveBeenCalled();
        expect(foldersService.createBasic).toHaveBeenCalled();
        expect(store.getters["mail-webapp/folders/getFolderByKey"](folderKey)).toBeDefined();
    });
});
