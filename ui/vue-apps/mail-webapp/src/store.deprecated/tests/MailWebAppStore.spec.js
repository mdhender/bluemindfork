import { MailboxesClient } from "@bluemind/mailbox.api";
import { createLocalVue } from "@vue/test-utils";
import {
    MockItemsTransferClient,
    MockMailboxItemsClient,
    MockMailboxFoldersClient,
    MockOwnerSubscriptionsClient,
    MockContainersClient,
    MockI18NProvider,
    MountComponentUtils
} from "@bluemind/test-utils";
import aliceFolders from "./data/alice/folders";
import aliceInbox from "./data/alice/inbox";
import cloneDeep from "lodash.clonedeep";
import containers from "./data/alice/containers";
import ItemUri from "@bluemind/item-uri";
import MailWebAppStore from "../";
import MailStore from "../../store/";
import AlertStore from "@bluemind/alert.store";
import { AlertFactory } from "@bluemind/alert.store";
import MailAppAlerts from "../../alerts";
import readOnlyFolders from "./data/read.only/folders";
import ServiceLocator from "@bluemind/inject";
import sharedFolders from "./data/shared/folders";
import Vuex from "vuex";
import WebsocketClient from "@bluemind/sockjs";
import { Flag } from "@bluemind/email";
import { FolderAdaptor } from "../../store/folders/helpers/FolderAdaptor";
import mutationTypes from "../../store/mutationTypes";
import MessageAdaptor from "../../store/messages/helpers/MessageAdaptor";
import { createOnlyMetadata } from "../../model/message";
import { create as createAttachment } from "../../model/attachment";

jest.mock("@bluemind/sockjs");
jest.mock("@bluemind/mailbox.api");
jest.mock("@bluemind/user.api");

let containerService = new MockContainersClient(),
    mailboxesService,
    foldersService = new MockMailboxFoldersClient(),
    itemsService = new MockMailboxItemsClient(),
    itemsTransferClient = new MockItemsTransferClient();
ServiceLocator.register({ provide: "ContainersPersistence", factory: () => containerService });
ServiceLocator.register({ provide: "SubscriptionPersistence", factory: () => new MockOwnerSubscriptionsClient() });
ServiceLocator.register({ provide: "MailboxesPersistence", factory: () => mailboxesService });
ServiceLocator.register({ provide: "MailboxItemsPersistence", factory: () => itemsService });
ServiceLocator.register({ provide: "MailboxFoldersPersistence", factory: () => foldersService });
ServiceLocator.register({ provide: "ItemsTransferPersistence", factory: () => itemsTransferClient });
ServiceLocator.register({
    provide: "UserSession",
    factory: () => {
        return { roles: ["hasCalendar"], userId: "6793466E-F5D4-490F-97BF-DF09D3327BF4" };
    }
});
ServiceLocator.register({ provide: "i18n", factory: () => MockI18NProvider });

WebsocketClient.register = jest.fn();
AlertFactory.register(MailAppAlerts);

const localVue = createLocalVue();
localVue.use(Vuex);

function initializeMessages({ commit }, messages, folderUid) {
    const messagesMetadata = messages.map(({ internalId }) =>
        createOnlyMetadata({ internalId, folder: { key: folderUid, uid: folderUid } })
    );
    commit("mail/" + mutationTypes.SET_MESSAGE_LIST, messagesMetadata);
    const adapted = messages.map(m => MessageAdaptor.fromMailboxItem(m, { key: folderUid }));
    commit("mail/" + mutationTypes.ADD_MESSAGES, adapted);
}

describe("[MailWebAppStore] Vuex store", () => {
    let store;

    beforeEach(() => {
        store = new Vuex.Store();
        store.registerModule("mail-webapp", cloneDeep(MailWebAppStore));
        store.registerModule("mail", cloneDeep(MailStore));
        store.registerModule("alert", cloneDeep(AlertStore));
        store.registerModule("session", cloneDeep(MountComponentUtils.mockSessionStore().modules.session));

        MailboxesClient.mockClear();
        foldersService = new MockMailboxFoldersClient();
        itemsService = new MockMailboxItemsClient();
        itemsTransferClient = new MockItemsTransferClient();
        containerService = new MockContainersClient();
        mailboxesService = new MailboxesClient();
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

        await store.dispatch("mail-webapp/bootstrap", "6793466E-F5D4-490F-97BF-DF09D3327BF4", { root: true });
        expect(store.state["mail-webapp"].userUid).toBe("6793466E-F5D4-490F-97BF-DF09D3327BF4");
        expect(store.getters["mail/MY_MAILBOX_KEY"]).toEqual("user.6793466E-F5D4-490F-97BF-DF09D3327BF4");
        expect(store.getters["mail/MY_INBOX"].key).toEqual("f1c3f42f-551b-446d-9682-cfe0574b3205");
        expect(store.getters["mail/MY_TRASH"].key).toEqual("98a9383e-5156-44eb-936a-e6b0825b0809");
        expect(store.getters["mail/MY_MAILBOX_FOLDERS"].length).toEqual(aliceFolders.length);

        await awaitForLastCall;
        expect(store.getters["mail/MAILSHARE_KEYS"].length).toBe(2);
        expect(store.getters["mail/MAILSHARE_FOLDERS"].length).toBe(4);
    });

    test("select a folder", async () => {
        //load data in new store
        containerService.getContainers.mockReturnValueOnce(Promise.resolve(containers));
        await store.dispatch("mail/FETCH_MAILBOXES");
        foldersService.all.mockReturnValueOnce(Promise.resolve(aliceFolders));
        await store.dispatch(
            "mail/FETCH_FOLDERS",
            store.state.mail.mailboxes["user.6793466E-F5D4-490F-97BF-DF09D3327BF4"]
        );
        store.commit("mail-webapp/setUserUid", "6793466E-F5D4-490F-97BF-DF09D3327BF4", { root: true });

        const folderUid = "050ca560-37ae-458a-bd52-c40fedf4068d";
        itemsService.sortedIds.mockReturnValueOnce(Promise.resolve(aliceInbox.map(message => message.internalId)));
        itemsService.getPerUserUnread.mockReturnValueOnce(Promise.resolve({ total: 4 }));
        itemsService.multipleById.mockImplementationOnce(ids =>
            Promise.resolve(aliceInbox.filter(message => ids.includes(message.internalId)))
        );
        await store.dispatch("mail-webapp/loadMessageList", { folder: folderUid }, { root: true });
        expect(store.state["mail"].activeFolder).toEqual(folderUid);
        expect(store.state.mail.messageList.messageKeys.length).toEqual(aliceInbox.length);
        expect(store.state.mail.messageList.messageKeys).toEqual(
            aliceInbox.map(m => ItemUri.encode(m.internalId, folderUid))
        );
        expect(store.state.mail.messages[store.state.mail.messageList.messageKeys[0]]).not.toBeUndefined();
    });

    //FIXME: When folder store became a module, this test broke
    test("select a folder with 'unread' filter", async () => {
        //load data in new store
        containerService.getContainers.mockReturnValueOnce(Promise.resolve(containers));
        await store.dispatch("mail/FETCH_MAILBOXES");
        foldersService.all.mockReturnValueOnce(Promise.resolve(aliceFolders));
        await store.dispatch(
            "mail/FETCH_FOLDERS",
            store.state.mail.mailboxes["user.6793466E-F5D4-490F-97BF-DF09D3327BF4"]
        );

        store.commit("mail-webapp/setUserUid", "6793466E-F5D4-490F-97BF-DF09D3327BF4", { root: true });

        const folderUid = "050ca560-37ae-458a-bd52-c40fedf4068d";
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

        expect(store.state.mail.messageList.messageKeys.length).toEqual(0);
        await store.dispatch("mail-webapp/loadMessageList", { folder: folderUid, filter: "unread" }, { root: true });
        expect(store.state.mail.activeFolder).toEqual(folderUid);
        expect(store.state.mail.messageList.messageKeys.length).toEqual(5);
        expect(store.state.mail.messageList.messageKeys).toEqual(
            aliceInbox
                .filter(message => !message.flags.includes("Seen"))
                .map(m => ItemUri.encode(m.internalId, folderUid))
        );
        expect(store.state.mail.messages[store.state.mail.messageList.messageKeys[0]]).not.toBeUndefined();
        expect(store.state.mail.folders[folderUid].unread).toBe(5);
        expect(store.getters["mail/CURRENT_MAILBOX"].key).toEqual("user.6793466E-F5D4-490F-97BF-DF09D3327BF4");
    });

    test("select a message", async () => {
        store.state.mail = {
            mailboxes: {
                "6793466E-F5D4-490F-97BF-DF09D3327BF4": {
                    key: "6793466E-F5D4-490F-97BF-DF09D3327BF4",
                    owner: "6793466E-F5D4-490F-97BF-DF09D3327BF4"
                }
            },
            folders: {
                "6793466E-F5D4-490F-97BF-DF09D3327BF4": {
                    mailboxRef: { key: "6793466E-F5D4-490F-97BF-DF09D3327BF4" },
                    imapName: "Drafts"
                }
            },
            messages: {},
            messageList: {}
        };

        const folderUid = "f1c3f42f-551b-446d-9682-cfe0574b3205";
        const messageKey = ItemUri.encode(872, folderUid);
        const text = "Text content...";
        itemsService.mockFetch(text);
        initializeMessages(store, aliceInbox, folderUid);
        await store.dispatch("mail-webapp/selectMessage", messageKey, { root: true });

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
        const attachment = createAttachment("2", "us-ascii", "api.rb", "7bit", "text/x-ruby-script", 28, true);
        expect(store.state["mail"].messages[messageKey].attachments).toEqual([attachment]);
    });

    test("remove a message from my mailbox", async () => {
        //load data in new store
        containerService.getContainers.mockReturnValueOnce(Promise.resolve(containers));
        await store.dispatch("mail/FETCH_MAILBOXES");
        foldersService.all.mockReturnValueOnce(Promise.resolve(aliceFolders));
        await store.dispatch(
            "mail/FETCH_FOLDERS",
            store.state.mail.mailboxes["user.6793466E-F5D4-490F-97BF-DF09D3327BF4"]
        );

        const folderUid = "f1c3f42f-551b-446d-9682-cfe0574b3205";
        let messageKey = ItemUri.encode(872, folderUid);
        store.commit("mail-webapp/setUserUid", "6793466E-F5D4-490F-97BF-DF09D3327BF4", { root: true });

        initializeMessages(store, aliceInbox, folderUid);

        store.commit("mail/SET_ACTIVE_FOLDER", folderUid, { root: true });
        expect(store.state.mail.messageList.messageKeys).toEqual(expect.arrayContaining([messageKey]));

        await store.dispatch("mail-webapp/remove", messageKey, { root: true });

        expect(store.state.mail.messageList.messageKeys).toEqual(expect.not.arrayContaining([messageKey]));
        expect(foldersService.importItems).toHaveBeenCalled();
    });

    test("remove a message from a mailshare", async () => {
        //load data
        containerService.getContainers.mockReturnValueOnce(Promise.resolve(containers));
        await store.dispatch("mail/FETCH_MAILBOXES");
        foldersService.all.mockReturnValueOnce(Promise.resolve(aliceFolders));
        await store.dispatch(
            "mail/FETCH_FOLDERS",
            store.state.mail.mailboxes["user.6793466E-F5D4-490F-97BF-DF09D3327BF4"]
        );
        foldersService.all.mockReturnValueOnce(Promise.resolve(sharedFolders));
        await store.dispatch("mail/FETCH_FOLDERS", store.state.mail.mailboxes["2814CC5D-D372-4F66-A434-89863E99B8CD"]);

        const folderUid = "f1c3f42f-551b-446d-9682-cfe0574b3205";
        store.state.mail.folders[folderUid].mailboxRef.key = "2814CC5D-D372-4F66-A434-89863E99B8CD";
        let messageKey = ItemUri.encode(872, folderUid);
        itemsService.fetchComplete.mockReturnValueOnce(Promise.resolve("eml"));
        itemsService.uploadPart.mockReturnValueOnce(Promise.resolve("addr"));
        store.commit("mail-webapp/setUserUid", "6793466E-F5D4-490F-97BF-DF09D3327BF4", { root: true });
        store.commit("mail/ADD_MAILBOXES", containers, { root: true });

        initializeMessages(store, aliceInbox, folderUid);
        store.commit("mail/SET_ACTIVE_FOLDER", folderUid, { root: true });
        expect(store.state.mail.messageList.messageKeys).toEqual(expect.arrayContaining([messageKey]));
        await store.dispatch("mail-webapp/remove", messageKey, { root: true });
        expect(store.state.mail.messageList.messageKeys).toEqual(expect.not.arrayContaining([messageKey]));
        expect(itemsTransferClient.move).toHaveBeenCalled();
    });

    test("remove a message from trash", async () => {
        //load data in new store
        containerService.getContainers.mockReturnValueOnce(Promise.resolve(containers));
        await store.dispatch("mail/FETCH_MAILBOXES");
        foldersService.all.mockReturnValueOnce(Promise.resolve(aliceFolders));
        await store.dispatch(
            "mail/FETCH_FOLDERS",
            store.state.mail.mailboxes["user.6793466E-F5D4-490F-97BF-DF09D3327BF4"]
        );

        const trashUid = "98a9383e-5156-44eb-936a-e6b0825b0809";
        const messageKey = ItemUri.encode(872, trashUid);
        store.commit("mail-webapp/setUserUid", "6793466E-F5D4-490F-97BF-DF09D3327BF4", { root: true });
        initializeMessages(store, aliceInbox, trashUid);
        expect(store.state.mail.messageList.messageKeys).toEqual(expect.arrayContaining([messageKey]));
        await store.dispatch("mail-webapp/remove", messageKey, { root: true });
        expect(store.state.mail.messageList.messageKeys).toEqual(expect.not.arrayContaining([messageKey]));
        expect(itemsService.multipleDeleteById).toHaveBeenCalled();
    });

    test("remove a message definitely", async () => {
        const folderUid = "f1c3f42f-551b-446d-9682-cfe0574b3205";
        const messageKey = ItemUri.encode(872, folderUid);
        store.commit("mail-webapp/setUserUid", "6793466E-F5D4-490F-97BF-DF09D3327BF4", { root: true });
        initializeMessages(store, aliceInbox, folderUid);
        expect(store.state.mail.messageList.messageKeys).toEqual(expect.arrayContaining([messageKey]));
        await store.dispatch("mail-webapp/purge", messageKey, { root: true });
        expect(store.state.mail.messageList.messageKeys).toEqual(expect.not.arrayContaining([messageKey]));
        expect(itemsService.multipleDeleteById).toHaveBeenCalled();
    });

    test("move a message", async () => {
        //load data in new store
        containerService.getContainers.mockReturnValueOnce(Promise.resolve(containers));
        await store.dispatch("mail/FETCH_MAILBOXES");
        foldersService.all.mockReturnValueOnce(Promise.resolve(aliceFolders));
        await store.dispatch(
            "mail/FETCH_FOLDERS",
            store.state.mail.mailboxes["user.6793466E-F5D4-490F-97BF-DF09D3327BF4"]
        );

        const inboxUid = "f1c3f42f-551b-446d-9682-cfe0574b3205";
        const archive2017Uid = "050ca560-37ae-458a-bd52-c40fedf4068d";
        let messageKey = ItemUri.encode(872, inboxUid);
        store.commit("mail-webapp/setUserUid", "6793466E-F5D4-490F-97BF-DF09D3327BF4", { root: true });
        store.commit("mail/SET_ACTIVE_FOLDER", inboxUid, { root: true });
        initializeMessages(store, aliceInbox, inboxUid);
        const destination = store.state.mail.folders[archive2017Uid];
        expect(store.state.mail.messageList.messageKeys).toEqual(expect.arrayContaining([messageKey]));
        await store.dispatch("mail-webapp/move", { messageKey, folder: destination }, { root: true });
        expect(store.state.mail.messageList.messageKeys).toEqual(expect.not.arrayContaining([messageKey]));
        expect(foldersService.importItems).toHaveBeenCalled();
    });

    test("move a message across mailboxes", async () => {
        //load data in new store
        containerService.getContainers.mockReturnValueOnce(Promise.resolve(containers));
        await store.dispatch("mail/FETCH_MAILBOXES");
        foldersService.all.mockReturnValueOnce(Promise.resolve(aliceFolders));
        await store.dispatch(
            "mail/FETCH_FOLDERS",
            store.state.mail.mailboxes["user.6793466E-F5D4-490F-97BF-DF09D3327BF4"]
        );
        foldersService.all.mockReturnValueOnce(Promise.resolve(sharedFolders));
        await store.dispatch("mail/FETCH_FOLDERS", store.state.mail.mailboxes["2814CC5D-D372-4F66-A434-89863E99B8CD"]);

        const folderUid = "f1c3f42f-551b-446d-9682-cfe0574b3205";
        const archive2017Uid = "050ca560-37ae-458a-bd52-c40fedf4068d";
        let messageKey = ItemUri.encode(872, folderUid);

        itemsService.fetchComplete.mockReturnValueOnce(Promise.resolve("eml"));
        itemsService.uploadPart.mockReturnValueOnce(Promise.resolve("addr"));

        store.commit("mail-webapp/setUserUid", "6793466E-F5D4-490F-97BF-DF09D3327BF4", { root: true });
        store.commit("mail/ADD_MAILBOXES", containers, { root: true });
        store.commit("mail/SET_ACTIVE_FOLDER", folderUid, { root: true });
        initializeMessages(store, aliceInbox, folderUid);
        store.state.mail.folders[archive2017Uid].mailboxRef.key = "2814CC5D-D372-4F66-A434-89863E99B8CD";
        const destination = store.state.mail.folders[archive2017Uid];
        expect(store.state.mail.messageList.messageKeys).toEqual(expect.arrayContaining([messageKey]));
        await store.dispatch("mail-webapp/move", { messageKey, folder: destination }, { root: true });
        expect(itemsTransferClient.move).toHaveBeenCalled();
        expect(store.state.mail.messageList.messageKeys).toEqual(expect.not.arrayContaining([messageKey]));
    });

    test("move a message in a new folder", async () => {
        //load data in new store
        containerService.getContainers.mockReturnValueOnce(Promise.resolve(containers));
        await store.dispatch("mail/FETCH_MAILBOXES");
        foldersService.all.mockReturnValueOnce(Promise.resolve(aliceFolders));
        await store.dispatch(
            "mail/FETCH_FOLDERS",
            store.state.mail.mailboxes["user.6793466E-F5D4-490F-97BF-DF09D3327BF4"]
        );

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
        const adaptedDestination = FolderAdaptor.fromMailboxFolder(destination, {
            remoteRef: {
                uid: "user.6793466E-F5D4-490F-97BF-DF09D3327BF4"
            }
        });
        adaptedDestination.key = undefined; // if a key is defined, it means folder is already created in server
        let folderUid = destination.uid;
        foldersService.createBasic.mockReturnValue(Promise.resolve({ uid: destination.uid }));
        foldersService.getComplete.mockReturnValue(Promise.resolve(destination));
        store.commit("mail-webapp/setUserUid", "6793466E-F5D4-490F-97BF-DF09D3327BF4", { root: true });
        initializeMessages(store, aliceInbox, inboxUid);
        store.commit("mail/SET_ACTIVE_FOLDER", folderUid, { root: true });

        await store.dispatch("mail-webapp/move", { messageKey, folder: adaptedDestination }, { root: true });

        expect(foldersService.importItems).toHaveBeenCalled();
        expect(foldersService.createBasic).toHaveBeenCalled();
        expect(store.state.mail.folders[folderUid]).toBeDefined();
        expect(store.state.mail.messageList.messageKeys).toEqual(expect.not.arrayContaining([messageKey]));
    });
});
