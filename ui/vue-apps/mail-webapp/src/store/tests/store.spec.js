import Vue from "vue";
import Vuex from "vuex";
import cloneDeep from "lodash.clonedeep";
import { Flag } from "@bluemind/email";
import storeData from "..";

import {
    ALL_CONVERSATIONS_ARE_SELECTED,
    ALL_SELECTED_CONVERSATIONS_ARE_FLAGGED,
    ALL_SELECTED_CONVERSATIONS_ARE_READ,
    ALL_SELECTED_CONVERSATIONS_ARE_UNFLAGGED,
    ALL_SELECTED_CONVERSATIONS_ARE_UNREAD,
    CONVERSATION_METADATA,
    CONVERSATIONS_ACTIVATED,
    FILTERED_MAILSHARE_RESULTS,
    FILTERED_USER_RESULTS,
    FOLDER_LIST_IS_EMPTY,
    MAILBOX_FOLDERS,
    MAILBOX_ROOT_FOLDERS,
    MAILBOX_SENT,
    MAILBOX_TRASH,
    MAILSHARE_FOLDERS,
    MAILSHARE_ROOT_FOLDERS,
    MY_DRAFTS,
    MY_INBOX,
    MY_MAILBOX_FOLDERS,
    MY_MAILBOX_ROOT_FOLDERS,
    MY_OUTBOX,
    MY_SENT,
    MY_TEMPLATES,
    MY_TRASH,
    NEXT_CONVERSATION,
    SELECTION
} from "~/getters";
import { DEFAULT_FOLDER_NAMES as NAMES } from "../folders/helpers/DefaultFolders";
import { MailboxType } from "~/model/mailbox";
import injector from "@bluemind/inject";
import { SET_FOLDER_FILTER_LOADED, SET_FOLDER_FILTER_LOADING, SET_ACTIVE_FOLDER } from "~/mutations";
import { LoadingStatus } from "../../model/loading-status";

Vue.use(Vuex);
injector.register({ provide: "UserSession", use: { userId: "B" } });

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
            store.state.mailboxes.keys = Object.keys(store.state.mailboxes);
            store.state.folders = {
                1: { key: "1", mailboxRef: { key: "B" } },
                2: { key: "2", mailboxRef: { key: "A" } }
            };
            store.commit(SET_ACTIVE_FOLDER, { key: "1" });
            expect(store.getters.CURRENT_MAILBOX).toEqual({ key: "B" });
        });
        test("ALL_SELECTED_CONVERSATIONS_ARE_UNREAD", () => {
            initMailbox(store);
            initConversations(store);
            store.state.conversations.messages[2].flags = [Flag.SEEN];
            store.state.conversations.messages[3].flags = [Flag.SEEN];
            store.state.selection._keys = [1, 4];
            expect(store.getters[ALL_SELECTED_CONVERSATIONS_ARE_UNREAD]).toBeTruthy();
            store.state.selection._keys = [1, 2, 3];
            expect(store.getters[ALL_SELECTED_CONVERSATIONS_ARE_UNREAD]).toBeFalsy();
            store.state.selection._keys = [];
            expect(store.getters[ALL_SELECTED_CONVERSATIONS_ARE_UNREAD]).toBeFalsy();
        });
        test("ALL_SELECTED_CONVERSATIONS_ARE_READ", () => {
            initMailbox(store);
            initConversations(store);
            store.state.conversations.messages[1].flags = [Flag.SEEN];
            store.state.conversations.messages[2].flags = [Flag.SEEN];
            store.state.selection._keys = [1, 2];
            expect(store.getters[ALL_SELECTED_CONVERSATIONS_ARE_READ]).toBeTruthy();
            store.state.selection._keys = [1, 2, 3];
            expect(store.getters[ALL_SELECTED_CONVERSATIONS_ARE_READ]).toBeFalsy();
            store.state.selection._keys = [];
            expect(store.getters[ALL_SELECTED_CONVERSATIONS_ARE_READ]).toBeFalsy();
        });
        test("ALL_SELECTED_CONVERSATIONS_ARE_FLAGGED", () => {
            initMailbox(store);
            initConversations(store);
            store.state.conversations.messages[1].flags = [Flag.FLAGGED];
            store.state.conversations.messages[2].flags = [Flag.FLAGGED];
            store.state.selection._keys = [1, 2];
            expect(store.getters[ALL_SELECTED_CONVERSATIONS_ARE_FLAGGED]).toBeTruthy();
            store.state.selection._keys = [1, 2, 3];
            expect(store.getters[ALL_SELECTED_CONVERSATIONS_ARE_FLAGGED]).toBeFalsy();
            store.state.selection._keys = [];
            expect(store.getters[ALL_SELECTED_CONVERSATIONS_ARE_FLAGGED]).toBeFalsy();
        });
        test("ALL_SELECTED_CONVERSATIONS_ARE_UNFLAGGED", () => {
            initMailbox(store);
            initConversations(store);
            store.state.conversations.messages[2].flags = [Flag.FLAGGED];
            store.state.conversations.messages[3].flags = [Flag.FLAGGED];
            store.state.selection._keys = [1, 4];
            expect(store.getters[ALL_SELECTED_CONVERSATIONS_ARE_UNFLAGGED]).toBeTruthy();
            store.state.selection._keys = [1, 2, 3];
            expect(store.getters[ALL_SELECTED_CONVERSATIONS_ARE_UNFLAGGED]).toBeFalsy();
            store.state.selection._keys = [];
            expect(store.getters[ALL_SELECTED_CONVERSATIONS_ARE_UNFLAGGED]).toBeFalsy();
        });
        test("ALL_CONVERSATIONS_ARE_SELECTED", () => {
            store.state.conversationList = { _keys: [1, 2, 3], _removed: [] };
            store.state.selection._keys = { _keys: [1, 2], _removed: [] };
            expect(store.getters[ALL_CONVERSATIONS_ARE_SELECTED]).toBeFalsy();
            store.state.selection._keys = [1, 2, 3];
            expect(store.getters[ALL_CONVERSATIONS_ARE_SELECTED]).toBeTruthy();
            store.state.selection._keys = [];
            store.state.conversationList = { _keys: [], _removed: [] };
            expect(store.getters[ALL_CONVERSATIONS_ARE_SELECTED]).toBeFalsy();
        });
        test("FILTERED_MAILSHARE_RESULTS", () => {
            store.state.folders = {
                "1": { key: "1", name: "", imapName: "a", path: "a", mailboxRef: { key: "A" } },
                "2": { key: "2", name: "", imapName: "bab", path: "a/bab", mailboxRef: { key: "A" } },
                "3": { key: "3", name: "", imapName: "a", path: "a", mailboxRef: { key: "B" }, parent: null },
                "4": { key: "4", name: "", imapName: "c", path: "c", mailboxRef: { key: "B" }, parent: "3" }
            };
            store.state.mailboxes = {
                A: { key: "A", type: MailboxType.MAILSHARE, owner: "B" },
                B: { key: "B", type: MailboxType.USER, owner: "B" }
            };

            store.state.mailboxes.keys = Object.keys(store.state.mailboxes);
            store.state.folderList.pattern = " ";
            store.commit(SET_FOLDER_FILTER_LOADED);
            expect(store.getters[FILTERED_MAILSHARE_RESULTS]).toEqual([]);
            store.state.folderList.pattern = "B";
            expect(store.getters[FILTERED_MAILSHARE_RESULTS]).toEqual([store.state.folders["2"]]);
            store.state.folderList.pattern = "C";
            expect(store.getters[FILTERED_MAILSHARE_RESULTS]).toEqual([]);
        });
        test("FILTERED_USER_RESULTS", () => {
            store.state.folders = {
                "1": { key: "1", name: "", imapName: "a", path: "a", mailboxRef: { key: "A" } },
                "2": { key: "2", name: "", imapName: "b", path: "a/b", mailboxRef: { key: "A" } },
                "3": { key: "3", name: "", imapName: "a", path: "a", mailboxRef: { key: "B" }, parent: null },
                "4": { key: "4", name: "", imapName: "c", path: "c", mailboxRef: { key: "B" }, parent: "3" },
                "5": { key: "4", name: "", imapName: "c", path: "c", mailboxRef: { key: "C" }, parent: "3" }
            };
            store.state.mailboxes = {
                A: { key: "A", type: MailboxType.MAILSHARE, owner: "B" },
                B: { key: "B", type: MailboxType.USER, owner: "B" },
                C: { key: "C", type: MailboxType.USER, owner: "C" }
            };
            store.state.mailboxes.keys = Object.keys(store.state.mailboxes);
            store.state.folderList.pattern = "";
            store.commit(SET_FOLDER_FILTER_LOADED);
            expect(store.getters[FILTERED_USER_RESULTS]).toEqual({});
            store.state.folderList.pattern = "A";
            expect(store.getters[FILTERED_USER_RESULTS]).toEqual({
                B: [store.state.folders["3"]],
                C: []
            });
            store.state.folderList.pattern = "c";
            expect(store.getters[FILTERED_USER_RESULTS]).toEqual({
                B: [store.state.folders["4"]],
                C: [store.state.folders["5"]]
            });
        });
        test("FOLDER_LIST_IS_EMPTY", () => {
            store.state.folders = {
                "1": { key: "1", name: "", imapName: "a", path: "a", mailboxRef: { key: "A" } },
                "2": { key: "2", name: "", imapName: "b", path: "a/b", mailboxRef: { key: "A" } },
                "3": { key: "3", name: "", imapName: "a", path: "a", mailboxRef: { key: "B" }, parent: null },
                "4": { key: "4", name: "", imapName: "c", path: "c", mailboxRef: { key: "B" }, parent: "3" },
                "5": { key: "4", name: "", imapName: "c", path: "c", mailboxRef: { key: "C" }, parent: "3" },
                "6": { key: "1", name: "", imapName: "y", path: "y", mailboxRef: { key: "A" } }
            };
            store.state.mailboxes = {
                A: { key: "A", type: MailboxType.MAILSHARE, owner: "B" },
                B: { key: "B", type: MailboxType.USER, owner: "B" },
                C: { key: "C", type: MailboxType.USER, owner: "C" }
            };
            store.state.mailboxes.keys = Object.keys(store.state.mailboxes);

            /* Not in filtered state */
            store.state.folderList.pattern = "";
            store.commit(SET_FOLDER_FILTER_LOADED);
            expect(store.getters[FOLDER_LIST_IS_EMPTY]).toBeTruthy();
            store.state.folderList.pattern = "a";
            store.commit(SET_FOLDER_FILTER_LOADING);
            expect(store.getters[FOLDER_LIST_IS_EMPTY]).toBeTruthy();
            /* Result in both user and mailshare */
            store.commit(SET_FOLDER_FILTER_LOADED);
            expect(store.getters[FOLDER_LIST_IS_EMPTY]).toBeFalsy();
            /* Result in none */
            store.state.folderList.pattern = "zzz";
            expect(store.getters[FOLDER_LIST_IS_EMPTY]).toBeTruthy();
            /* Result in mailshares */
            store.state.folderList.pattern = "y";
            expect(store.getters[FOLDER_LIST_IS_EMPTY]).toBeFalsy();
            /* Result in user */
            store.state.folderList.pattern = "c";
            expect(store.getters[FOLDER_LIST_IS_EMPTY]).toBeFalsy();
        });
        test("MAILSHARE_FOLDERS (sorted by mailshare dn)", () => {
            store.state.folders = {
                "1": { key: "1", mailboxRef: { key: "A" } },
                "2": { key: "2", mailboxRef: { key: "unknown" } },
                "3": { key: "3", mailboxRef: { key: "B" } },
                "4": { key: "4", mailboxRef: { key: "C" } }
            };
            store.state.mailboxes = {
                A: { key: "A", type: MailboxType.MAILSHARE, dn: "zzz" },
                C: { key: "C", type: MailboxType.MAILSHARE, dn: "aaa" }
            };
            store.state.mailboxes.keys = Object.keys(store.state.mailboxes);

            expect(store.getters[MAILSHARE_FOLDERS]).toEqual([store.state.folders["4"], store.state.folders["1"]]);
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
            store.state.mailboxes.keys = Object.keys(store.state.mailboxes);
            injector.register({
                provide: "UserSession",
                use: { userId: "B" }
            });
            expect(store.getters[MY_MAILBOX_FOLDERS]).toEqual([store.state.folders["3"]]);
        });

        test("DEFAULT FOLDERS", () => {
            store.state.folders = {
                "1": {
                    key: "1",
                    default: false,
                    imapName: "whatever",
                    name: "whathever",
                    path: "whathever",
                    mailboxRef: { key: "myMailbox" }
                },
                "1bis": {
                    key: "1bis",
                    default: true,
                    imapName: NAMES.INBOX,
                    path: NAMES.INBOX,
                    mailboxRef: { key: "other" }
                },
                "2": {
                    key: "2",
                    default: true,
                    imapName: NAMES.INBOX,
                    path: NAMES.INBOX,
                    mailboxRef: { key: "myMailbox" }
                },
                "3": {
                    key: "3",
                    default: true,
                    imapName: NAMES.OUTBOX,
                    path: NAMES.OUTBOX,
                    mailboxRef: { key: "myMailbox" }
                },
                "4": {
                    key: "4",
                    default: true,
                    imapName: NAMES.SENT,
                    path: NAMES.SENT,
                    mailboxRef: { key: "myMailbox" }
                },
                "5": {
                    key: "5",
                    default: true,
                    imapName: NAMES.TRASH,
                    path: NAMES.TRASH,
                    mailboxRef: { key: "myMailbox" }
                },
                "6": {
                    key: "6",
                    default: true,
                    imapName: NAMES.DRAFTS,
                    path: NAMES.DRAFTS,
                    mailboxRef: { key: "myMailbox" }
                },
                "7": {
                    key: "7",
                    default: true,
                    imapName: NAMES.TEMPLATES,
                    path: NAMES.TEMPLATES,
                    mailboxRef: { key: "myMailbox" }
                }
            };
            store.state.mailboxes = {
                myMailbox: {
                    key: "myMailbox",
                    owner: "me",
                    loading: LoadingStatus.LOADED
                }
            };
            store.state.mailboxes.keys = Object.keys(store.state.mailboxes);
            store.state.mailboxes.folders = {
                defaults: {
                    myMailbox: {
                        [NAMES.INBOX]: "2",
                        [NAMES.OUTBOX]: "3",
                        [NAMES.DRAFTS]: "6",
                        [NAMES.SENT]: "4",
                        [NAMES.TRASH]: "5",
                        [NAMES.TEMPLATES]: "7"
                    }
                }
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
            expect(store.getters[MY_TEMPLATES].key).toEqual("7");
        });
        test("MY_MAILBOX_ROOT_FOLDERS", () => {
            store.state.folders = {
                "1": { key: "1", imapName: "a", path: "a", mailboxRef: { key: "A" } },
                "2": { key: "2", imapName: "b", path: "b", mailboxRef: { key: "unknown" } },
                "3": { key: "3", imapName: "c", path: "c", mailboxRef: { key: "B" }, parent: null },
                "4": { key: "4", imapName: "d", path: "d", mailboxRef: { key: "B" }, parent: "3" }
            };
            store.state.mailboxes = {
                B: { key: "B", owner: "B" }
            };
            store.state.mailboxes.keys = Object.keys(store.state.mailboxes);
            injector.register({
                provide: "UserSession",
                use: { userId: "B" }
            });
            expect(store.getters[MY_MAILBOX_ROOT_FOLDERS]).toEqual([store.state.folders["3"]]);
        });
        test("MAILBOX_ROOT_FOLDERS", () => {
            store.state.folders = {
                "1": { key: "1", imapName: "a", path: "a", mailboxRef: { key: "A" } },
                "2": { key: "2", imapName: "b", path: "b", mailboxRef: { key: "C" }, parent: null },
                "3": { key: "3", imapName: "c", path: "c", mailboxRef: { key: "B" }, parent: null },
                "4": { key: "4", imapName: "d", path: "d", mailboxRef: { key: "B" }, parent: "3" },
                "5": { key: "5", imapName: "b", path: "b", mailboxRef: { key: "C" }, parent: "2" },
                "6": { key: "6", imapName: "b", path: "b", mailboxRef: { key: "C" }, parent: null }
            };
            store.state.mailboxes = {
                B: { key: "B", owner: "B" },
                C: { key: "C", owner: "C" }
            };
            store.state.mailboxes.keys = Object.keys(store.state.mailboxes);
            expect(store.getters[MAILBOX_ROOT_FOLDERS]({ key: "B" })).toEqual([store.state.folders["3"]]);
            expect(store.getters[MAILBOX_ROOT_FOLDERS]({ key: "C" })).toEqual([
                store.state.folders["2"],
                store.state.folders["6"]
            ]);
        });
        test("MAILBOX_FOLDERS", () => {
            store.state.folders = {
                "1": { key: "1", imapName: "a", path: "a", mailboxRef: { key: "A" } },
                "2": { key: "2", imapName: "b", path: "b", mailboxRef: { key: "unknown" } },
                "3": { key: "3", imapName: "c", path: "c", mailboxRef: { key: "B" }, parent: null },
                "4": { key: "4", imapName: "d", path: "d", mailboxRef: { key: "B" }, parent: "3" }
            };

            expect(store.getters[MAILBOX_FOLDERS]({ key: "A" })).toEqual([store.state.folders["1"]]);
            expect(store.getters[MAILBOX_FOLDERS]({ key: "B" })).toEqual([
                store.state.folders["3"],
                store.state.folders["4"]
            ]);
        });
        test("MAILBOX_TRASH", () => {
            store.state.folders = {
                "1": {
                    key: "1",
                    default: true,
                    imapName: NAMES.TRASH,
                    path: `other/${NAMES.TRASH}`,
                    mailboxRef: { key: "other" }
                },
                "2": {
                    key: "2",
                    default: true,
                    imapName: NAMES.TRASH,
                    path: NAMES.TRASH,
                    mailboxRef: { key: "next" }
                }
            };
            store.state.mailboxes = {
                other: { key: "other", owner: "me", loading: LoadingStatus.LOADED },
                next: { key: "next", owner: "me", loading: LoadingStatus.LOADED }
            };
            store.state.mailboxes.keys = Object.keys(store.state.mailboxes);
            store.state.mailboxes.folders = {
                defaults: { other: { [NAMES.TRASH]: "1" }, next: { [NAMES.TRASH]: "2" } }
            };
            expect(store.getters[MAILBOX_TRASH]({ key: "other" }).key).toEqual("1");
            expect(store.getters[MAILBOX_TRASH]({ key: "next" }).key).toEqual("2");
        });
        test("MAILBOX_SENT", () => {
            store.state.folders = {
                "1": {
                    key: "1",
                    default: false,
                    imapName: "whatever",
                    name: "whathever",
                    path: "whathever",
                    mailboxRef: { key: "other" }
                },
                "1bis": {
                    key: "1bis",
                    default: true,
                    imapName: NAMES.SENT,
                    path: `other/${NAMES.SENT}`,
                    mailboxRef: { key: "other" }
                },
                "2": {
                    key: "2",
                    default: true,
                    imapName: NAMES.SENT,
                    path: `next/${NAMES.SENT}`,
                    mailboxRef: { key: "next" }
                }
            };
            store.state.mailboxes = {
                other: { key: "other", owner: "me", loading: LoadingStatus.LOADED },
                next: { key: "next", owner: "me", loading: LoadingStatus.LOADED }
            };
            store.state.mailboxes.keys = ["other", "next"];
            store.state.mailboxes.folders = { defaults: { other: { [NAMES.SENT]: "1" }, next: { [NAMES.SENT]: "2" } } };
            expect(store.getters[MAILBOX_SENT]({ key: "other" }).key).toEqual("1");
            expect(store.getters[MAILBOX_SENT]({ key: "next" }).key).toEqual("2");
        });
        test("MAILSHARE_ROOT_FOLDERS (sorted by mailshare dn)", () => {
            store.state.folders = {
                "1": { key: "1", mailboxRef: { key: "A" }, parent: null },
                "2": { key: "2", mailboxRef: { key: "unknown" }, parent: null },
                "3": { key: "3", mailboxRef: { key: "B" }, parent: "1" },
                "4": { key: "4", mailboxRef: { key: "C" }, parent: "4" },
                "5": { key: "5", mailboxRef: { key: "D" }, parent: null }
            };
            store.state.mailboxes = {
                A: { key: "A", type: MailboxType.MAILSHARE, dn: "zzz" },
                D: { key: "D", type: MailboxType.MAILSHARE, dn: "aaa" }
            };
            store.state.mailboxes.keys = Object.keys(store.state.mailboxes);
            expect(store.getters[MAILSHARE_ROOT_FOLDERS]).toEqual([store.state.folders["5"], store.state.folders["1"]]);
        });
        describe("NEXT_CONVERSATION", () => {
            beforeEach(() => {
                store.state.conversationList._keys = Array(10)
                    .fill(0)
                    .map((v, i) => 2 * i);
                store.state.conversations.conversationByKey = store.state.conversationList._keys.reduce(
                    (obj, key) => ({ ...obj, [key]: { key, folderRef: { key: "folderKey" }, messages: ["k1", "k2"] } }),
                    {}
                );
                store.state.conversations.messages = {
                    k1: { key: "k1", folderRef: { key: "folderKey" } },
                    k2: { key: "k2", folderRef: { key: "folderKey" } }
                };
                store.state.selection._keys = [];
                store.state.conversations.currentConversation = undefined;
                store.state.mailboxes = {
                    myMailbox: { key: "myMailbox", owner: "me", loading: LoadingStatus.LOADED }
                };
                store.state.mailboxes.keys = Object.keys(store.state.mailboxes);
                store.state.mailboxes.folders = {
                    defaults: {
                        myMailbox: {
                            [NAMES.INBOX]: "2",
                            [NAMES.OUTBOX]: "3",
                            [NAMES.SENT]: "4",
                            [NAMES.TRASH]: "5",
                            [NAMES.DRAFTS]: "6"
                        },
                        other: {
                            [NAMES.INBOX]: "1bis"
                        }
                    }
                };
                injector.register({
                    provide: "UserSession",
                    use: { userId: "me" }
                });
                store.state.folders = {
                    "1": {
                        key: "1",
                        imapName: "whatever",
                        path: "whatever",
                        mailboxRef: { key: "myMailbox" }
                    },
                    "1bis": {
                        key: "1bis",
                        imapName: NAMES.INBOX,
                        default: true,
                        path: NAMES.INBOX,
                        mailboxRef: { key: "other" }
                    },
                    "2": {
                        key: "2",
                        imapName: NAMES.INBOX,
                        default: true,
                        path: NAMES.INBOX,
                        mailboxRef: { key: "myMailbox" }
                    },
                    "3": {
                        key: "3",
                        imapName: NAMES.OUTBOX,
                        default: true,
                        path: NAMES.OUTBOX,
                        mailboxRef: { key: "myMailbox" }
                    },
                    "4": {
                        key: "4",
                        imapName: NAMES.SENT,
                        default: true,
                        path: NAMES.SENT,
                        mailboxRef: { key: "myMailbox" }
                    },
                    "5": {
                        key: "5",
                        imapName: NAMES.TRASH,
                        default: true,
                        path: NAMES.TRASH,
                        mailboxRef: { key: "myMailbox" }
                    },
                    "6": {
                        key: "6",
                        imapName: NAMES.DRAFTS,
                        default: true,
                        path: NAMES.DRAFTS,
                        mailboxRef: { key: "myMailbox" }
                    }
                };
            });
            test("return first conversation after current conversation", () => {
                store.state.conversations.currentConversation = 2;
                const conversations = [{ key: store.state.conversations.currentConversation }];
                const result = store.getters[NEXT_CONVERSATION](conversations);
                expect(result.key).toBe(4);
            });
            test("return the previous conversation if there is no next conversation ", () => {
                store.state.conversations.currentConversation = 18;
                const conversations = [{ key: store.state.conversations.currentConversation }];
                const result = store.getters[NEXT_CONVERSATION](conversations);
                expect(result.key).toBe(16);
            });
            test("return null if there is no next conversation", () => {
                store.state.conversationList._keys = [2];
                store.state.conversations.currentConversation = 2;
                const conversations = [{ key: store.state.conversations.currentConversation }];
                const result = store.getters[NEXT_CONVERSATION](conversations);
                expect(result).toBeNull();
            });
            test("return null if no currentConversation", () => {
                store.state.conversations.currentConversation = undefined;
                const result = store.getters[NEXT_CONVERSATION]([]);
                expect(result).toBeNull();
            });
            test("return null conversation if current conversation is not in list", () => {
                store.state.conversations.conversationByKey[20] = {
                    key: 20,
                    remoteRef: { internalId: 1 },
                    folderRef: { key: 1 }
                };
                store.state.conversations.currentConversation = 20;
                const conversations = [{ key: store.state.conversations.currentConversation }];
                const result = store.getters[NEXT_CONVERSATION](conversations);
                expect(result).toBeNull();
            });
        });
        test("SELECTION", () => {
            store.state.folders = {
                "1": { key: "1", imapName: "whatever", path: "wathever", mailboxRef: { key: "myMailbox" } },
                "2": {
                    key: "2",
                    default: true,
                    imapName: NAMES.INBOX,
                    path: NAMES.INBOX,
                    mailboxRef: { key: "myMailbox" }
                },
                "5": {
                    key: "5",
                    default: true,
                    imapName: NAMES.TRASH,
                    path: NAMES.TRASH,
                    mailboxRef: { key: "myMailbox" }
                },
                "6": {
                    key: "6",
                    default: true,
                    imapName: NAMES.SENT,
                    path: NAMES.SENT,
                    mailboxRef: { key: "myMailbox" }
                }
            };
            store.state.mailboxes = {
                myMailbox: { key: "myMailbox", owner: "me", loading: LoadingStatus.LOADED }
            };
            store.state.mailboxes.keys = Object.keys(store.state.mailboxes);
            store.state.mailboxes.folders = {
                defaults: {
                    myMailbox: {
                        [NAMES.INBOX]: "2",
                        [NAMES.TRASH]: "5",
                        [NAMES.SENT]: "6"
                    }
                }
            };
            injector.register({ provide: "UserSession", use: { userId: "me" } });
            Array.from(Array(100)).forEach((v, key) => {
                if (key % 2 === 0) {
                    store.state.selection._keys.push(key);
                }
                if (key % 10 === 0) {
                    store.state.selection._removed.push(key);
                }
                store.state.conversations.conversationByKey[key] = { key, folderRef: { key: 1 }, messages: [key] };
                store.state.conversations.messages[key] = { key, folderRef: { key: 1 } };
            });
            expect(store.getters[SELECTION]).toEqual(
                Object.values(store.state.conversations.conversationByKey)
                    .filter(({ key }) => key % 2 === 0 && key % 10 !== 0)
                    .map(({ key }) => store.getters[CONVERSATION_METADATA](key))
            );
            expect(store.getters[SELECTION].length).toEqual(40);
            expect(store.getters[SELECTION][0]).toEqual(
                store.getters[CONVERSATION_METADATA](store.state.selection._keys[1])
            );
        });
        test("CONVERSATIONS_ACTIVATED", () => {
            store.state.mailThreadSetting = "true";
            store.state.activeFolder = "activeFolderKey";
            store.state.folders[store.state.activeFolder] = { allowConversations: true };
            store.state.conversationList.search.pattern = "";
            expect(store.getters[CONVERSATIONS_ACTIVATED]).toBeTruthy();

            store.state.mailThreadSetting = "false";
            store.state.activeFolder = "activeFolderKey";
            store.state.folders[store.state.activeFolder] = { allowConversations: true };
            store.state.conversationList.search.pattern = "";
            expect(store.getters[CONVERSATIONS_ACTIVATED]).toBeFalsy();

            store.state.mailThreadSetting = "true";
            store.state.activeFolder = "activeFolderKey";
            store.state.folders[store.state.activeFolder] = { allowConversations: false };
            store.state.conversationList.search.pattern = "";
            expect(store.getters[CONVERSATIONS_ACTIVATED]).toBeFalsy();

            store.state.mailThreadSetting = "true";
            store.state.activeFolder = "activeFolderKey";
            store.state.folders[store.state.activeFolder] = { allowConversations: true };
            store.state.conversationList.search.pattern = "searchPattern";
            expect(store.getters[CONVERSATIONS_ACTIVATED]).toBeFalsy();
        });
    });
});

function initMailbox(store) {
    store.state.folders = {
        "1": {
            key: "1",
            imapName: NAMES.TRASH,
            mailboxRef: { key: "myMailbox", remoteRef: { uid: "toto" } }
        }
    };
    store.state.mailboxes = {
        B: { key: "B", owner: "B", remoteRef: { uid: "toto" } }
    };
    store.state.mailboxes.keys = Object.keys(store.state.mailboxes);
    injector.register({
        provide: "UserSession",
        use: { userId: "B" }
    });
    injector.register({ provide: "i18n", use: { t: n => n } });
}

function initConversations(store) {
    const folderRef = { key: "folderKey" };
    const conversations = [
        {
            key: 1,
            folderRef,
            messages: [1],
            loading: LoadingStatus.LOADED,
            remoteRef: { uid: "a1b2c3d4e5f61" }
        },
        {
            key: 2,
            folderRef,
            messages: [2],
            loading: LoadingStatus.NOT_LOADED,
            remoteRef: { uid: "a1b2c3d4e5f62" }
        },
        {
            key: 3,
            folderRef,
            messages: [3],
            loading: LoadingStatus.LOADED,
            remoteRef: { uid: "a1b2c3d4e5f63" }
        },
        {
            key: 4,
            folderRef,
            messages: [4],
            loading: LoadingStatus.LOADED,
            remoteRef: { uid: "a1b2c3d4e5f64" }
        }
    ];
    const messages = [
        { key: 1, folderRef, flags: [], loading: LoadingStatus.LOADED },
        { key: 2, folderRef, flags: [], loading: LoadingStatus.LOADED },
        { key: 3, folderRef, flags: [], loading: LoadingStatus.LOADED },
        { key: 4, folderRef, flags: [], loading: LoadingStatus.LOADED }
    ];
    store.commit("SET_CONVERSATION_LIST", { conversations, messages });
}
