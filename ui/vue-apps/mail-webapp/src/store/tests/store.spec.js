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
    CURRENT_MAILBOX,
    MAILSHARE_FOLDERS,
    MAILSHARE_ROOT_FOLDERS,
    CONVERSATION_LIST_CONVERSATIONS,
    MY_DRAFTS,
    MY_INBOX,
    MY_MAILBOX,
    MY_MAILBOX_FOLDERS,
    MY_MAILBOX_ROOT_FOLDERS,
    MY_OUTBOX,
    MY_SENT,
    MY_TRASH,
    NEXT_CONVERSATION,
    SELECTION
} from "~/getters";
import { DEFAULT_FOLDER_NAMES } from "../folders/helpers/DefaultFolders";
import { MailboxType } from "~/model/mailbox";
import injector from "@bluemind/inject";
import { SET_ACTIVE_FOLDER } from "~/mutations";
import { LoadingStatus } from "../../model/loading-status";

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
        test("ALL_SELECTED_CONVERSATIONS_ARE_UNREAD", () => {
            initMailbox(store);
            store.state.conversations.conversationByKey = buildConversations([[], [Flag.SEEN], [Flag.SEEN], []]);
            initConversations(store);
            store.state.selection = [1, 4];
            expect(store.getters[ALL_SELECTED_CONVERSATIONS_ARE_UNREAD]).toBeTruthy();
            store.state.selection = [1, 2, 3];
            expect(store.getters[ALL_SELECTED_CONVERSATIONS_ARE_UNREAD]).toBeFalsy();
            store.state.selection = [];
            expect(store.getters[ALL_SELECTED_CONVERSATIONS_ARE_UNREAD]).toBeFalsy();
        });
        test("ALL_SELECTED_CONVERSATIONS_ARE_READ", () => {
            initMailbox(store);
            store.state.conversations.conversationByKey = buildConversations([[Flag.SEEN], [Flag.SEEN], [], []]);
            initConversations(store);
            store.state.selection = [1, 2];
            expect(store.getters[ALL_SELECTED_CONVERSATIONS_ARE_READ]).toBeTruthy();
            store.state.selection = [1, 2, 3];
            expect(store.getters[ALL_SELECTED_CONVERSATIONS_ARE_READ]).toBeFalsy();
            store.state.selection = [];
            expect(store.getters[ALL_SELECTED_CONVERSATIONS_ARE_READ]).toBeFalsy();
        });
        test("ALL_SELECTED_CONVERSATIONS_ARE_FLAGGED", () => {
            initMailbox(store);
            store.state.conversations.conversationByKey = buildConversations([[Flag.FLAGGED], [Flag.FLAGGED], [], []]);
            initConversations(store);
            store.state.selection = [1, 2];
            expect(store.getters[ALL_SELECTED_CONVERSATIONS_ARE_FLAGGED]).toBeTruthy();
            store.state.selection = [1, 2, 3];
            expect(store.getters[ALL_SELECTED_CONVERSATIONS_ARE_FLAGGED]).toBeFalsy();
            store.state.selection = [];
            expect(store.getters[ALL_SELECTED_CONVERSATIONS_ARE_FLAGGED]).toBeFalsy();
        });
        test("ALL_SELECTED_CONVERSATIONS_ARE_UNFLAGGED", () => {
            initMailbox(store);
            store.state.conversations.conversationByKey = buildConversations([[], [Flag.FLAGGED], [Flag.FLAGGED], []]);
            initConversations(store);
            store.state.selection = [1, 4];
            expect(store.getters[ALL_SELECTED_CONVERSATIONS_ARE_UNFLAGGED]).toBeTruthy();
            store.state.selection = [1, 2, 3];
            expect(store.getters[ALL_SELECTED_CONVERSATIONS_ARE_UNFLAGGED]).toBeFalsy();
            store.state.selection = [];
            expect(store.getters[ALL_SELECTED_CONVERSATIONS_ARE_UNFLAGGED]).toBeFalsy();
        });
        test("ALL_CONVERSATIONS_ARE_SELECTED", () => {
            store.state.conversationList = { _keys: [1, 2, 3], _removed: [] };
            store.state.selection = { _keys: [1, 2], _removed: [] };
            expect(store.getters[ALL_CONVERSATIONS_ARE_SELECTED]).toBeFalsy();
            store.state.selection._keys = [1, 2, 3];
            expect(store.getters[ALL_CONVERSATIONS_ARE_SELECTED]).toBeTruthy();
            store.state.selection._keys = [];
            store.state.conversationList = { _keys: [], _removed: [] };
            expect(store.getters[ALL_CONVERSATIONS_ARE_SELECTED]).toBeFalsy();
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

        // skipped because unable to mock CURRENT_MAILBOX
        test.skip("CONVERSATION_LIST_CONVERSATIONS", () => {
            Array.from(Array(500)).forEach((v, key) => {
                if (key % 2 === 0) {
                    store.state.conversationList._keys.push(key);
                }
                if (key % 10 === 0) {
                    store.state.conversationList._removed.push(key);
                }
                const folderRef = { key: "folderKey" };
                store.state.conversations.messages[key] = { key, folderRef };
                store.state.conversations.conversationByKey[key] = {
                    key,
                    messages: [{ key, folderRef }]
                };
            });

            // unable to mock CURRENT_MAILBOX FIXME
            store.getters = {
                ...store.getters,
                [MY_MAILBOX]: { loading: LoadingStatus.LOADED, writable: true },
                [CURRENT_MAILBOX]: { writable: true }
            };

            store.state.conversationList.currentPage = 0;
            expect(store.getters[CONVERSATION_LIST_CONVERSATIONS]).toEqual([]);
            store.state.conversationList.currentPage = 1;
            expect(store.getters[CONVERSATION_LIST_CONVERSATIONS].length).toEqual(50);
            expect(store.getters[CONVERSATION_LIST_CONVERSATIONS][0]).toEqual(
                store.state.conversations.conversationByKey[store.state.conversationList._keys[1]]
            );
            store.state.conversationList.currentPage = 2;
            expect(store.getters[CONVERSATION_LIST_CONVERSATIONS].length).toEqual(50);
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
                myMailbox: { key: "myMailbox", owner: "me", loading: LoadingStatus.LOADED }
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
        describe("NEXT_CONVERSATION", () => {
            beforeEach(() => {
                store.state.conversationList._keys = Array(10)
                    .fill(0)
                    .map((v, i) => 2 * i);
                store.state.conversations.conversationByKey = store.state.conversationList._keys.reduce(
                    (obj, key) => ({ ...obj, [key]: { key } }),
                    {}
                );
                store.state.selection._keys = [];
                store.state.conversations.currentConversation = undefined;
            });
            test("return first conversation after current conversation", () => {
                store.state.conversations.currentConversation = { key: 2 };
                const result = store.getters[NEXT_CONVERSATION];
                expect(result.key).toBe(4);
            });
            test("return the previous conversation if there is no next conversation ", () => {
                store.state.conversations.currentConversation = { key: 18 };
                const result = store.getters[NEXT_CONVERSATION];
                expect(result.key).toBe(16);
            });
            test("return null if there is no next conversation", () => {
                store.state.conversationList._keys = [2];
                store.state.conversations.currentConversation = { key: 2 };
                const result = store.getters[NEXT_CONVERSATION];
                expect(result).toBeNull();
            });
            test("return null if no currentConversation", () => {
                store.state.conversations.currentConversation = undefined;
                const result = store.getters[NEXT_CONVERSATION];
                expect(result).toBeNull();
            });
            test("return first conversation if current conversation is not in list", () => {
                store.state.conversations.conversationByKey[20] = {
                    key: 20,
                    remoteRef: { internalId: 1 },
                    folderRef: { key: 1 }
                };
                store.state.conversations.currentConversation = { key: 20 };
                const result = store.getters[NEXT_CONVERSATION];
                expect(result.key).toBe(0);
            });
            test("do not return first conversation if it is equal to current conversation", () => {
                store.state.conversations.currentConversation = {
                    key: 20,
                    remoteRef: { internalId: 1 },
                    folderRef: { key: 1 }
                };
                store.state.conversations.conversationByKey[0] = {
                    key: 0,
                    remoteRef: { internalId: 1 },
                    folderRef: { key: 1 }
                };
                const result = store.getters[NEXT_CONVERSATION];
                expect(result.key).toBe(2);
            });
        });
        test("SELECTION", () => {
            store.state.folders = {
                "1": { key: "1", imapName: "whatever", mailboxRef: { key: "myMailbox" } },
                "2": { key: "2", imapName: DEFAULT_FOLDER_NAMES.INBOX, mailboxRef: { key: "myMailbox" } },
                "5": { key: "5", imapName: DEFAULT_FOLDER_NAMES.TRASH, mailboxRef: { key: "myMailbox" } },
                "6": { key: "6", imapName: DEFAULT_FOLDER_NAMES.SENT, mailboxRef: { key: "myMailbox" } }
            };
            store.state.mailboxes = {
                myMailbox: { key: "myMailbox", owner: "me", loading: LoadingStatus.LOADED }
            };
            injector.register({ provide: "UserSession", use: { userId: "me" } });
            Array.from(Array(100)).forEach((v, key) => {
                if (key % 2 === 0) {
                    store.state.selection._keys.push(key);
                }
                if (key % 10 === 0) {
                    store.state.selection._removed.push(key);
                }
                store.state.conversations.conversationByKey[key] = { key, folderRef: { key: 1 }, messages: [{ key }] };
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
    });
});

function initMailbox(store) {
    store.state.folders = {
        "1": {
            key: "1",
            imapName: DEFAULT_FOLDER_NAMES.TRASH,
            mailboxRef: { key: "myMailbox", remoteRef: { uid: "toto" } }
        }
    };
    store.state.mailboxes = {
        B: { key: "B", owner: "B", remoteRef: { uid: "toto" } }
    };
    injector.register({
        provide: "UserSession",
        use: { userId: "B" }
    });
    injector.register({ provide: "i18n", use: { t: n => n } });
}

function buildConversations(flags) {
    const folderRef = { key: "folderKey" };
    return {
        1: {
            key: 1,
            folderRef,
            messages: [{ key: 1, folderRef, flags: flags[0], loading: LoadingStatus.LOADED }],
            loading: LoadingStatus.LOADED
        },
        2: {
            key: 2,
            folderRef,
            messages: [{ key: 2, folderRef, flags: flags[1], loading: LoadingStatus.LOADED }],
            loading: LoadingStatus.NOT_LOADED
        },
        3: {
            key: 3,
            folderRef,
            messages: [{ key: 3, folderRef, flags: flags[2], loading: LoadingStatus.LOADED }],
            loading: LoadingStatus.LOADED
        },
        4: {
            key: 4,
            folderRef,
            messages: [{ key: 4, folderRef, flags: flags[3], loading: LoadingStatus.LOADED }],
            loading: LoadingStatus.LOADED
        }
    };
}

function initConversations(store) {
    store.commit("SET_CONVERSATION_LIST", Object.values(store.state.conversations.conversationByKey));
    const flattenMessages = Object.values(store.state.conversations.conversationByKey).flatMap(c => c.messages);
    store.commit("ADD_MESSAGES", flattenMessages);
}
