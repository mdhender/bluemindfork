import Vue from "vue";
import Vuex from "vuex";
import cloneDeep from "lodash.clonedeep";
import { Flag } from "@bluemind/email";
import storeData from "..";

import {
    ALL_MESSAGES_ARE_SELECTED,
    ALL_SELECTED_MESSAGES_ARE_FLAGGED,
    ALL_SELECTED_MESSAGES_ARE_READ,
    ALL_SELECTED_MESSAGES_ARE_UNFLAGGED,
    ALL_SELECTED_MESSAGES_ARE_UNREAD,
    MAILSHARE_FOLDERS,
    MAILSHARE_ROOT_FOLDERS,
    MY_DRAFTS,
    MY_INBOX,
    MY_MAILBOX_FOLDERS,
    MY_MAILBOX_ROOT_FOLDERS,
    MY_OUTBOX,
    MY_SENT,
    MY_TRASH
} from "~getters";
import { MessageStatus } from "~model/message";
import { DEFAULT_FOLDER_NAMES } from "../folders/helpers/DefaultFolders";
import { MailboxType } from "~model/mailbox";
import injector from "@bluemind/inject";
import { SET_ACTIVE_FOLDER } from "~mutations";

Vue.use(Vuex);

describe("Mail store", () => {
    let store;
    beforeEach(() => {
        store = new Vuex.Store(cloneDeep(storeData));
    });
    describe("store.mutations", () => {
        test("SET_ACTIVE_FOLDER: define the active folder", () => {
            store.commit(SET_ACTIVE_FOLDER, { key: "1" });
            expect(store.state.activeFolder).toEqual("1");

            store.commit(SET_ACTIVE_FOLDER, { key: "2" });
            expect(store.state.activeFolder).toEqual("2");
        });
    });

    describe("store.getters", () => {
        test("CURRENT_MAILBOX: return mailbox object matching activeFolder", () => {
            store.state.mailboxes = {
                A: { key: "A" },
                B: { key: "B" }
            };
            store.state.folders = {
                1: { key: "1", mailboxRef: { key: "B" } },
                2: { key: "2", mailboxRef: { key: "A" } }
            };
            store.commit(SET_ACTIVE_FOLDER, { key: "1" });
            expect(store.getters.CURRENT_MAILBOX).toEqual({ key: "B" });
        });
        test("ALL_SELECTED_MESSAGES_ARE_UNREAD", () => {
            store.state.messages = {
                1: { flags: [], status: MessageStatus.LOADED },
                2: { flags: [Flag.SEEN], status: MessageStatus.NOT_LOADED },
                3: { flags: [Flag.SEEN], status: MessageStatus.LOADED },
                4: { flags: [], status: MessageStatus.LOADED }
            };
            store.state.selection = [1, 2];
            expect(store.getters[ALL_SELECTED_MESSAGES_ARE_UNREAD]).toBeTruthy();
            store.state.selection = [1, 2, 3];
            expect(store.getters[ALL_SELECTED_MESSAGES_ARE_UNREAD]).toBeFalsy();
            store.state.selection = [];
            expect(store.getters[ALL_SELECTED_MESSAGES_ARE_UNREAD]).toBeFalsy();
        });
        test("ALL_SELECTED_MESSAGES_ARE_READ", () => {
            store.state.messages = {
                1: { flags: [Flag.SEEN], status: MessageStatus.LOADED },
                2: { flags: [], status: MessageStatus.NOT_LOADED },
                3: { flags: [], status: MessageStatus.LOADED },
                4: { flags: [Flag.SEEN], status: MessageStatus.LOADED }
            };
            store.state.selection = [1, 2];
            expect(store.getters[ALL_SELECTED_MESSAGES_ARE_READ]).toBeTruthy();
            store.state.selection = [1, 2, 3];
            expect(store.getters[ALL_SELECTED_MESSAGES_ARE_READ]).toBeFalsy();
            store.state.selection = [];
            expect(store.getters[ALL_SELECTED_MESSAGES_ARE_READ]).toBeFalsy();
        });
        test("ALL_SELECTED_MESSAGES_ARE_FLAGGED", () => {
            store.state.messages = {
                1: { flags: [Flag.FLAGGED], status: MessageStatus.LOADED },
                2: { flags: [], status: MessageStatus.NOT_LOADED },
                3: { flags: [], status: MessageStatus.LOADED },
                4: { flags: [Flag.FLAGGED], status: MessageStatus.LOADED }
            };
            store.state.selection = [1, 2];
            expect(store.getters[ALL_SELECTED_MESSAGES_ARE_FLAGGED]).toBeTruthy();
            store.state.selection = [1, 2, 3];
            expect(store.getters[ALL_SELECTED_MESSAGES_ARE_FLAGGED]).toBeFalsy();
            store.state.selection = [];
            expect(store.getters[ALL_SELECTED_MESSAGES_ARE_FLAGGED]).toBeFalsy();
        });
        test("ALL_SELECTED_MESSAGES_ARE_UNFLAGGED", () => {
            store.state.messages = {
                1: { flags: [], status: MessageStatus.LOADED },
                2: { flags: [Flag.FLAGGED], status: MessageStatus.NOT_LOADED },
                3: { flags: [Flag.FLAGGED], status: MessageStatus.LOADED },
                4: { flags: [], status: MessageStatus.LOADED }
            };
            store.state.selection = [1, 2];
            expect(store.getters[ALL_SELECTED_MESSAGES_ARE_UNFLAGGED]).toBeTruthy();
            store.state.selection = [1, 2, 3];
            expect(store.getters[ALL_SELECTED_MESSAGES_ARE_UNFLAGGED]).toBeFalsy();
            store.state.selection = [];
            expect(store.getters[ALL_SELECTED_MESSAGES_ARE_UNFLAGGED]).toBeFalsy();
        });
        test("ALL_MESSAGES_ARE_SELECTED", () => {
            store.state.messageList = { messageKeys: [1, 2, 3] };
            store.state.selection = [1, 2];
            expect(store.getters[ALL_MESSAGES_ARE_SELECTED]).toBeFalsy();
            store.state.selection = [1, 2, 3];
            expect(store.getters[ALL_MESSAGES_ARE_SELECTED]).toBeTruthy();
            store.state.selection = [];
            store.state.messageList = { messageKeys: [] };
            expect(store.getters[ALL_MESSAGES_ARE_SELECTED]).toBeFalsy();
        });

        test("MAILSHARE_FOLDERS", () => {
            store.state.folders = {
                "1": { key: "1", mailboxRef: { key: "A" } },
                "2": { key: "2", mailboxRef: { key: "unknown" } },
                "3": { key: "3", mailboxRef: { key: "B" } },
                "4": { key: "4", mailboxRef: { key: "C" } }
            };
            store.state.mailboxes = {
                A: { key: "A", type: MailboxType.MAILSHARE },
                C: { key: "C", type: MailboxType.MAILSHARE }
            };
            expect(store.getters[MAILSHARE_FOLDERS]).toEqual([store.state.folders["1"], store.state.folders["4"]]);
        });

        test("MY_MAILBOX_FOLDERS", () => {
            store.state.folders = {
                "1": { key: "1", mailboxRef: { key: "A" } },
                "2": { key: "2", mailboxRef: { key: "unknown" } },
                "3": { key: "3", mailboxRef: { key: "B" } },
                "4": { key: "4", mailboxRef: { key: "C" } }
            };
            store.state.mailboxes = {
                B: { key: "B", owner: "B" }
            };
            injector.register({
                provide: "UserSession",
                use: { userId: "B" }
            });
            expect(store.getters[MY_MAILBOX_FOLDERS]).toEqual([store.state.folders["3"]]);
        });

        test("DEFAULT FOLDERS", () => {
            store.state.folders = {
                "1": { key: "1", imapName: "whatever", mailboxRef: { key: "myMailbox" } },
                "1bis": { key: "1bis", imapName: DEFAULT_FOLDER_NAMES.INBOX, mailboxRef: { key: "other" } },
                "2": { key: "2", imapName: DEFAULT_FOLDER_NAMES.INBOX, mailboxRef: { key: "myMailbox" } },
                "3": { key: "3", imapName: DEFAULT_FOLDER_NAMES.OUTBOX, mailboxRef: { key: "myMailbox" } },
                "4": { key: "4", imapName: DEFAULT_FOLDER_NAMES.SENT, mailboxRef: { key: "myMailbox" } },
                "5": { key: "5", imapName: DEFAULT_FOLDER_NAMES.TRASH, mailboxRef: { key: "myMailbox" } },
                "6": { key: "6", imapName: DEFAULT_FOLDER_NAMES.DRAFTS, mailboxRef: { key: "myMailbox" } }
            };
            store.state.mailboxes = {
                myMailbox: { key: "myMailbox", owner: "me" }
            };
            injector.register({
                provide: "UserSession",
                use: { userId: "me" }
            });
            expect(store.getters[MY_INBOX].key).toEqual("2");
            expect(store.getters[MY_OUTBOX].key).toEqual("3");
            expect(store.getters[MY_DRAFTS].key).toEqual("6");
            expect(store.getters[MY_SENT].key).toEqual("4");
            expect(store.getters[MY_TRASH].key).toEqual("5");
        });
        test("MY_MAILBOX_ROOT_FOLDERS", () => {
            store.state.folders = {
                "1": { key: "1", mailboxRef: { key: "A" } },
                "2": { key: "2", mailboxRef: { key: "unknown" } },
                "3": { key: "3", mailboxRef: { key: "B" }, parent: null },
                "4": { key: "4", mailboxRef: { key: "B" }, parent: "3" }
            };
            store.state.mailboxes = {
                B: { key: "B", owner: "B" }
            };
            injector.register({
                provide: "UserSession",
                use: { userId: "B" }
            });
            expect(store.getters[MY_MAILBOX_ROOT_FOLDERS]).toEqual([store.state.folders["3"]]);
        });
        test("MAILSHARE_ROOT_FOLDERS", () => {
            store.state.folders = {
                "1": { key: "1", mailboxRef: { key: "A" }, parent: null },
                "2": { key: "2", mailboxRef: { key: "unknown" }, parent: null },
                "3": { key: "3", mailboxRef: { key: "B" }, parent: "1" },
                "4": { key: "4", mailboxRef: { key: "C" }, parent: "4" }
            };
            store.state.mailboxes = {
                A: { key: "A", type: MailboxType.MAILSHARE },
                C: { key: "C", type: MailboxType.MAILSHARE }
            };
            expect(store.getters[MAILSHARE_ROOT_FOLDERS]).toEqual([store.state.folders["1"]]);
        });
    });
});
