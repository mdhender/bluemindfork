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
    MOVE_CONVERSATIONS
} from "~/actions";
import { REMOVE_CONVERSATIONS, REMOVE_MESSAGES, ADD_MESSAGES, SET_CURRENT_CONVERSATION } from "~/mutations";
import { default as storeOptions } from "../conversations";
import { Flag } from "@bluemind/email";
import { LoadingStatus } from "~/model/loading-status";
import ServiceLocator from "@bluemind/inject";
import { inject } from "@bluemind/inject";
import { MockMailboxFoldersClient, MockMailboxItemsClient } from "@bluemind/test-utils";

jest.mock("../api/apiMessages");

Vue.use(Vuex);

const folder = { key: "folderKey1", remoteRef: { uid: "folderKey1" } };
const messagesData = [
    { key: "messageKey1", remoteRef: { internalId: "internalId1" }, date: 1 },
    { key: "messageKey2", remoteRef: { internalId: "internalId2" }, date: 2 },
    { key: "messageKey3", remoteRef: { internalId: "internalId3" }, date: 3 },
    { key: "messageKey4", remoteRef: { internalId: "internalId4" }, date: 4 },
    { key: "messageKey5", remoteRef: { internalId: "internalId5" }, date: 5 },
    { key: "messageKey6", remoteRef: { internalId: "internalId6" }, date: 6 },
    { key: "messageKey7", remoteRef: { internalId: "internalId7" }, date: 7 },
    { key: "messageKey8", remoteRef: { internalId: "internalId8" }, date: 8 },
    { key: "messageKey9", remoteRef: { internalId: "internalId9" }, date: 9 }
];
const conversationsData = [
    {
        key: "key1",
        folderRef: { key: folder.key },
        remoteRef: { internalId: "internalId1" },
        messages: ["messageKey1", "messageKey2", "messageKey3"]
    },
    {
        key: "key2",
        folderRef: { key: folder.key },
        remoteRef: { internalId: "internalId4" },
        messages: ["messageKey4", "messageKey5", "messageKey6"]
    },
    {
        key: "key3",
        folderRef: { key: folder.key },
        remoteRef: { internalId: "internalId7" },
        messages: ["messageKey7", "messageKey8", "messageKey9"]
    }
];

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
        ServiceLocator.register({ provide: "MailboxItemsPersistence", use: new MockMailboxItemsClient(messagesData) });
        ServiceLocator.register({ provide: "MailboxFoldersPersistence", use: new MockMailboxFoldersClient() });
        storeOptions.actions["alert/LOADING"] = jest.fn();
        storeOptions.actions["alert/SUCCESS"] = jest.fn();
        storeOptions.actions["alert/ERROR"] = jest.fn();
        store = new Vuex.Store(cloneDeep(storeOptions));
        store.getters.MY_TRASH = { key: "trashKey" };
        store.getters.MY_SENT = { key: "sentKey" };
        store.state.conversationByKey = {};
        const folderRef = { key: folder.key };
        store.state.messages = {};
        cloneDeep(messagesData).forEach(msg => {
            store.state.messages[msg.key] = { ...msg, folderRef, loading: LoadingStatus.LOADED, flags: [] };
        });
        cloneDeep(conversationsData).forEach(conv => {
            store.state.conversationByKey[conv.key] = { ...conv, folderRef, loading: LoadingStatus.LOADED, flags: [] };
            conv.messages.forEach(
                key => (store.state.messages[key].conversationRef = { key: conv.key, id: conv.remoteRef.internalId })
            );
        });
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
        test("MOVE_CONVERSATIONS", () => {
            const conversations = [store.state.conversationByKey["key1"], store.state.conversationByKey["key3"]];
            storeOptions.actions[MOVE_CONVERSATIONS](store, { conversations, folder: { key: "targetFolderKey" } });
            expect(apiMessages.move).toHaveBeenCalled();
        });
        test("REMOVE_CONVERSATIONS", () => {
            const conversations = [store.state.conversationByKey["key1"], store.state.conversationByKey["key3"]];
            const spy = jest.fn();
            storeOptions.actions[REMOVE_CONVERSATIONS](
                { getters: store.getters, dispatch: spy, commit: spy, state: store.state },
                { conversations }
            );
            expect(apiMessages.addFlag).toHaveBeenCalled();
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

        test("SET_CURRENT_CONVERSATION", () => {
            expect(store.state.currentConversation).toBeFalsy();
            storeOptions.mutations[SET_CURRENT_CONVERSATION](store.state, { key: "key2" });
            expect(store.state.currentConversation).toBeTruthy();
            expect(store.state.currentConversation).toBe("key2");
        });
    });
    describe("hooks", () => {
        test("REMOVE_MESSAGES", () => {
            storeOptions.mutations[REMOVE_MESSAGES](store.state, {
                messages: [{ key: "messageKey5", conversationRef: { key: "key2" }, date: 5 }]
            });
            expect(store.state.conversationByKey["key2"].messages.length).toBe(2);
            expect(store.state.conversationByKey["key2"].messages.includes("messageKey5")).toBeFalsy();
        });
        test("ADD_MESSAGES", () => {
            storeOptions.mutations[ADD_MESSAGES](store.state, {
                messages: [{ key: "messageKey10", conversationRef: { key: "key2" } }]
            });
            expect(store.state.conversationByKey["key2"].messages.length).toBe(4);
            expect(store.state.conversationByKey["key2"].messages.includes("messageKey10")).toBeTruthy();
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
            expect(store.getters[CONVERSATION_MESSAGE_BY_KEY]("key2")).toMatchInlineSnapshot(`
                Array [
                  Object {
                    "conversationRef": Object {
                      "id": "internalId4",
                      "key": "key2",
                    },
                    "date": 4,
                    "flags": Array [],
                    "folderRef": Object {
                      "key": "folderKey1",
                    },
                    "key": "messageKey4",
                    "loading": "LOADED",
                    "remoteRef": Object {
                      "internalId": "internalId4",
                    },
                  },
                  Object {
                    "conversationRef": Object {
                      "id": "internalId4",
                      "key": "key2",
                    },
                    "date": 5,
                    "flags": Array [],
                    "folderRef": Object {
                      "key": "folderKey1",
                    },
                    "key": "messageKey5",
                    "loading": "LOADED",
                    "remoteRef": Object {
                      "internalId": "internalId5",
                    },
                  },
                  Object {
                    "conversationRef": Object {
                      "id": "internalId4",
                      "key": "key2",
                    },
                    "date": 6,
                    "flags": Array [],
                    "folderRef": Object {
                      "key": "folderKey1",
                    },
                    "key": "messageKey6",
                    "loading": "LOADED",
                    "remoteRef": Object {
                      "internalId": "internalId6",
                    },
                  },
                ]
            `);
        });
    });
});
