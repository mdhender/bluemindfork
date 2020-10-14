import Vue from "vue";
import Vuex from "vuex";
import cloneDeep from "lodash.clonedeep";
import apiMessages from "../api/apiMessages";
import { CONVERSATION_IS_LOADED, CONVERSATION_MESSAGE_BY_KEY } from "~/getters";
import {
    EMPTY_FOLDER,
    MARK_CONVERSATIONS_AS_FLAGGED,
    MARK_CONVERSATIONS_AS_READ,
    MARK_CONVERSATIONS_AS_UNFLAGGED,
    MARK_CONVERSATIONS_AS_UNREAD,
    MOVE_CONVERSATIONS,
    MOVE_CONVERSATIONS_TO_TRASH,
    MOVE_MESSAGES_NO_ALERT
} from "~/actions";
import {
    ADD_MESSAGE_TO_CONVERSATION,
    REMOVE_CONVERSATIONS,
    REMOVE_NEW_MESSAGE_FROM_CONVERSATION,
    SET_CURRENT_CONVERSATION
} from "~/mutations";
import { default as storeOptions } from "../conversations";
import { Flag } from "@bluemind/email";
import { LoadingStatus } from "~/model/loading-status";
import ServiceLocator from "@bluemind/inject";
import { inject } from "@bluemind/inject";
import { MockMailboxFoldersClient } from "@bluemind/test-utils";

jest.mock("../api/apiMessages");

Vue.use(Vuex);

const folder = { key: "folderKey1", remoteRef: { uid: "folderKey1" } };

const conversationByKey = {
    key1: {
        key: "key1",
        folderRef: { key: folder.key },
        remoteRef: { internalId: "internalId1" },
        messages: [
            { key: "messageKey1", remoteRef: { internalId: "internalId1" } },
            { key: "messageKey2", remoteRef: { internalId: "internalId2" } },
            { key: "messageKey3", remoteRef: { internalId: "internalId3" } }
        ]
    },
    key2: {
        key: "key2",
        folderRef: { key: folder.key },
        remoteRef: { internalId: "internalId4" },
        messages: [
            { key: "messageKey4", remoteRef: { internalId: "internalId4" } },
            { key: "messageKey5", remoteRef: { internalId: "internalId5" } },
            { key: "messageKey6", remoteRef: { internalId: "internalId6" } }
        ]
    },
    key3: {
        key: "key3",
        folderRef: { key: folder.key },
        remoteRef: { internalId: "internalId7" },
        messages: [
            { key: "messageKey7", remoteRef: { internalId: "internalId7" } },
            { key: "messageKey8", remoteRef: { internalId: "internalId8" } },
            { key: "messageKey9", remoteRef: { internalId: "internalId9" } }
        ]
    }
};

describe("conversations", () => {
    let store;
    const mailbox = {
        type: "",
        name: "",
        remoteRef: {}
    };

    beforeEach(() => {
        ServiceLocator.register({
            provide: "MailConversationPersistence",
            use: {
                byFolder: jest.fn(() => [
                    { value: { messageRefs: [{ itemId: "internalIdNEW", folderUid: folder.key }] } }
                ])
            }
        });
        ServiceLocator.register({ provide: "MailboxFoldersPersistence", use: new MockMailboxFoldersClient() });
        storeOptions.actions["alert/LOADING"] = jest.fn();
        storeOptions.actions["alert/SUCCESS"] = jest.fn();
        storeOptions.actions["alert/ERROR"] = jest.fn();
        store = new Vuex.Store(cloneDeep(storeOptions));
        store.getters.MY_TRASH = { key: "trashKey" };
        store.getters.MY_SENT = { key: "sentKey" };
        store.state.conversationByKey = cloneDeep(conversationByKey);
        store.state.messages = cloneDeep(simulateLoaded(Object.values(store.state.conversationByKey)));
        store.commit("SET_CONVERSATION_LIST", Object.values(conversationByKey));
    });

    describe("actions", () => {
        test("MARK_CONVERSATIONS_AS_READ", async () => {
            const conversations = [store.state.conversationByKey["key1"], store.state.conversationByKey["key3"]];
            await store.dispatch(MARK_CONVERSATIONS_AS_READ, { conversations });
            const metadatas = ["key1", "key2", "key3"]
                .map(key => store.getters["CONVERSATION_METADATA"](key))
                .filter(Boolean);
            expect(metadatas[0].flags).toContain(Flag.SEEN);
            expect(metadatas[1].flags).not.toContain(Flag.SEEN);
            expect(metadatas[2].flags).toContain(Flag.SEEN);
        });
        test("MARK_CONVERSATIONS_AS_UNREAD", async () => {
            let conversations = [
                store.state.conversationByKey["key1"],
                store.state.conversationByKey["key2"],
                store.state.conversationByKey["key3"]
            ];
            await store.dispatch(MARK_CONVERSATIONS_AS_READ, { conversations });

            conversations = [store.state.conversationByKey["key1"], store.state.conversationByKey["key3"]];
            await store.dispatch(MARK_CONVERSATIONS_AS_UNREAD, { conversations });
            const metadatas = ["key1", "key2", "key3"]
                .map(key => store.getters["CONVERSATION_METADATA"](key))
                .filter(Boolean);
            expect(metadatas[0].flags).not.toContain(Flag.SEEN);
            expect(metadatas[1].flags).toContain(Flag.SEEN);
            expect(metadatas[2].flags).not.toContain(Flag.SEEN);
        });
        test("MARK_CONVERSATIONS_AS_FLAGGED", async () => {
            const conversations = [store.state.conversationByKey["key1"], store.state.conversationByKey["key3"]];
            await store.dispatch(MARK_CONVERSATIONS_AS_FLAGGED, { conversations });
            const metadatas = ["key1", "key2", "key3"]
                .map(key => store.getters["CONVERSATION_METADATA"](key))
                .filter(Boolean);
            expect(metadatas[0].flags).toContain(Flag.FLAGGED);
            expect(metadatas[1].flags).not.toContain(Flag.FLAGGED);
            expect(metadatas[2].flags).toContain(Flag.FLAGGED);
        });
        test("MARK_CONVERSATIONS_AS_UNFLAGGED", async () => {
            let conversations = [
                store.state.conversationByKey["key1"],
                store.state.conversationByKey["key2"],
                store.state.conversationByKey["key3"]
            ];
            await store.dispatch(MARK_CONVERSATIONS_AS_FLAGGED, { conversations });

            conversations = [store.state.conversationByKey["key1"], store.state.conversationByKey["key3"]];
            await store.dispatch(MARK_CONVERSATIONS_AS_UNFLAGGED, { conversations });
            const metadatas = ["key1", "key2", "key3"]
                .map(key => store.getters["CONVERSATION_METADATA"](key))
                .filter(Boolean);
            expect(metadatas[0].flags).not.toContain(Flag.FLAGGED);
            expect(metadatas[1].flags).toContain(Flag.FLAGGED);
            expect(metadatas[2].flags).not.toContain(Flag.FLAGGED);
        });
        test("MOVE_CONVERSATIONS_TO_TRASH", () => {
            const conversations = [store.state.conversationByKey["key1"], store.state.conversationByKey["key3"]];
            const spy = jest.fn();
            storeOptions.actions[MOVE_CONVERSATIONS_TO_TRASH](
                { getters: store.getters, dispatch: spy, state: store.state },
                { conversations, folder: { key: "trashFolderKey" } }
            );
            expect(spy).toHaveBeenCalledWith(MOVE_MESSAGES_NO_ALERT, expect.anything());
        });
        test("MOVE_CONVERSATIONS", () => {
            const conversations = [store.state.conversationByKey["key1"], store.state.conversationByKey["key3"]];
            const spy = jest.fn();
            storeOptions.actions[MOVE_CONVERSATIONS](
                { getters: store.getters, dispatch: spy, state: store.state },
                { conversations, folder: { key: "targetFolderKey" } }
            );
            expect(spy).toHaveBeenCalledWith(MOVE_MESSAGES_NO_ALERT, expect.anything());
        });
        test("REMOVE_CONVERSATIONS", () => {
            const conversations = [store.state.conversationByKey["key1"], store.state.conversationByKey["key3"]];
            const spy = jest.fn();
            storeOptions.actions[REMOVE_CONVERSATIONS](
                { getters: store.getters, dispatch: spy, commit: spy, state: store.state },
                { conversations }
            );
            // keys = conversations
            expect(apiMessages.multipleDeleteById).toHaveBeenCalled();
        });

        describe("EMPTY_FOLDER", () => {
            test("Call remote API", async () => {
                store.dispatch(EMPTY_FOLDER, { folder, mailbox });
                expect(inject("MailboxFoldersPersistence").removeMessages).toHaveBeenCalledWith(
                    folder.remoteRef.internalId
                );
            });
            test("Remove messages synchronously", () => {
                const sample = Object.values(store.state.messages)[0];
                store.dispatch(EMPTY_FOLDER, { folder, mailbox });
                expect(store.state.messages[sample.key]).toBeUndefined();
            });
            test("Remove messages from store after the remote call", async () => {
                const sample = Object.values(store.state.messages)[0];
                expect(store.state.messages[sample.key]).toBeDefined();
                await store.dispatch(EMPTY_FOLDER, { folder, mailbox });
                expect(store.state.messages[sample.key]).toBeUndefined();
            });
            test("Restore messages if API call fails", async () => {
                const sample = Object.values(store.state.messages)[0];
                inject("MailboxFoldersPersistence").removeMessages.mockRejectedValueOnce("Failure");
                try {
                    await store.dispatch(EMPTY_FOLDER, { folder, mailbox });
                } catch {
                    // nothing to do
                } finally {
                    expect(store.state.messages[sample.key]).toBeDefined();
                }
            });
        });
    });
    describe("mutations", () => {
        test("REMOVE_CONVERSATION", () => {
            storeOptions.mutations[REMOVE_CONVERSATIONS](store.state, [{ key: "key2" }]);
            expect(store.state.conversationByKey["key2"]).toBeFalsy();
        });
        test("ADD_MESSAGE_TO_CONVERSATION", () => {
            storeOptions.mutations[ADD_MESSAGE_TO_CONVERSATION](store.state, {
                message: { key: "newKey" },
                conversation: { key: "key2" }
            });
            expect(store.state.conversationByKey["key2"].messages.length).toBe(4);
            expect(store.state.conversationByKey["key2"].messages[3].key).toBe("newKey");
        });
        test("REMOVE_NEW_MESSAGE_FROM_CONVERSATION", () => {
            storeOptions.mutations[REMOVE_NEW_MESSAGE_FROM_CONVERSATION](store.state, {
                message: { key: "messageKey5" },
                conversation: { key: "key2" }
            });
            expect(store.state.conversationByKey["key2"].messages.length).toBe(2);
            expect(store.state.conversationByKey["key2"].messages.some(m => m.key === "messageKey5")).toBeFalsy();
        });
        test("SET_CURRENT_CONVERSATION", () => {
            expect(store.state.currentConversation).toBeFalsy();
            storeOptions.mutations[SET_CURRENT_CONVERSATION](store.state, { key: "key2" });
            expect(store.state.currentConversation).toBeTruthy();
            expect(store.state.currentConversation.key).toBe("key2");
        });
    });
    describe("getters", () => {
        test("CONVERSATION_IS_LOADED", () => {
            expect(store.getters[CONVERSATION_IS_LOADED]({ loading: LoadingStatus.LOADED })).toBeTruthy();
            expect(store.getters[CONVERSATION_IS_LOADED]({ loading: LoadingStatus.NOT_LOADED })).toBeFalsy();
            expect(store.getters[CONVERSATION_IS_LOADED]({ loading: LoadingStatus.LOADING })).toBeFalsy();
            expect(store.getters[CONVERSATION_IS_LOADED]({ loading: LoadingStatus.ERROR })).toBeFalsy();
            expect(store.getters[CONVERSATION_IS_LOADED]({})).toBeFalsy();
        });
        test("CONVERSATION_MESSAGE_BY_KEY", () => {
            expect(store.getters[CONVERSATION_MESSAGE_BY_KEY]("key2")).toEqual([
                {
                    key: "messageKey4",
                    remoteRef: { internalId: "internalId4" },
                    folderRef: { key: folder.key },
                    flags: [],
                    loading: "LOADED"
                },
                {
                    key: "messageKey5",
                    remoteRef: { internalId: "internalId5" },
                    folderRef: { key: folder.key },
                    flags: [],
                    loading: "LOADED"
                },
                {
                    key: "messageKey6",
                    remoteRef: { internalId: "internalId6" },
                    folderRef: { key: folder.key },
                    flags: [],
                    loading: "LOADED"
                }
            ]);
        });
    });
});

function simulateLoaded(conversations) {
    return Object.fromEntries(
        conversations.flatMap(c => {
            c.flags = [];
            const folderRef = { key: folder.key };
            c.folderRef = folderRef;
            c.loading = LoadingStatus.LOADED;
            return c.messages.map(m => {
                m.folderRef = folderRef;
                return [m.key, { ...m, loading: LoadingStatus.LOADED, flags: [] }];
            });
        })
    );
}
