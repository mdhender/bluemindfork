import Vue from "vue";
import Vuex from "vuex";
import cloneDeep from "lodash.clonedeep";

import { Flag } from "@bluemind/email";
import ServiceLocator, { inject } from "@bluemind/inject";
import { MockMailboxItemsClient, MockMailboxFoldersClient, MockItemsTransferClient } from "@bluemind/test-utils";
import { messageUtils, loadingStatusUtils } from "@bluemind/mail";

import messageStore from "../../index";
import { ADD_MESSAGES } from "~/mutations";
import {
    ADD_FLAG,
    DELETE_FLAG,
    FETCH_MESSAGE_METADATA,
    REMOVE_MESSAGES,
    MOVE_MESSAGES,
    MARK_MESSAGES_AS_FLAGGED,
    MARK_MESSAGES_AS_READ,
    MARK_MESSAGES_AS_UNFLAGGED,
    MARK_MESSAGES_AS_UNREAD
} from "~/actions";
import { FETCH_MESSAGE_IF_NOT_LOADED } from "~/actions";
import { FolderAdaptor } from "~/store/folders/helpers/FolderAdaptor";

// FIXME: move it in global setup ?
jest.mock("@bluemind/i18n", () => {
    return { t: () => "" };
});
const { LoadingStatus } = loadingStatusUtils;
const { MessageAdaptor, MessageStatus, createOnlyMetadata, createWithMetadata } = messageUtils;
Vue.use(Vuex);

describe("Messages actions", () => {
    let store;
    let folder = { key: "folder-key", remoteRef: { uid: "folder-key" } };
    let messages;

    beforeEach(() => {
        messageStore.actions["alert/LOADING"] = jest.fn();
        messageStore.actions["alert/SUCCESS"] = jest.fn();
        messageStore.actions["alert/ERROR"] = jest.fn();
        store = new Vuex.Store(cloneDeep(messageStore));
        ServiceLocator.register({ provide: "MailboxItemsPersistence", use: new MockMailboxItemsClient(messages) });
        ServiceLocator.register({ provide: "ItemsTransferPersistence", use: new MockItemsTransferClient() });
        ServiceLocator.register({ provide: "MailboxFoldersPersistence", use: new MockMailboxFoldersClient() });
        messages = cloneDeep(require("../../../tests/data/users/alice/messages.json"));
    });

    describe("ADD_FLAG", () => {
        test("Call add flag remote API", async () => {
            const adapted = messages
                .filter(({ value: { flags } }) => !flags.includes(Flag.SEEN))
                .slice(0, 5)
                .map(m => MessageAdaptor.fromMailboxItem(m, folder));
            store.commit(ADD_MESSAGES, { messages: adapted });
            await store.dispatch(ADD_FLAG, {
                messages: adapted,
                flag: Flag.SEEN
            });
            const itemsId = adapted.map(message => message.remoteRef.internalId);
            expect(inject("MailboxItemsPersistence").addFlag).toHaveBeenCalledWith({
                itemsId,
                mailboxItemFlag: Flag.SEEN
            });
        });
        test("Call add flag remote API only for message whithout [flag]", async () => {
            const adapted = messages
                .filter(({ value: { flags } }) => flags.includes(Flag.SEEN))
                .slice(0, 5)
                .map(m => MessageAdaptor.fromMailboxItem(m, folder));
            store.commit(ADD_MESSAGES, { messages: adapted });
            await store.dispatch(ADD_FLAG, {
                messages: adapted,
                flag: Flag.SEEN
            });
            expect(inject("MailboxItemsPersistence").addFlag).not.toHaveBeenCalled();
        });
        test("Call add flag remote API for messages not yet loaded", async () => {
            const adapted = [1, 2].map(id => createOnlyMetadata({ internalId: id, folder }));
            store.commit(ADD_MESSAGES, { messages: adapted });
            await store.dispatch(ADD_FLAG, {
                messages: adapted,
                flag: Flag.SEEN
            });
            expect(inject("MailboxItemsPersistence").addFlag).toHaveBeenCalledWith({
                itemsId: [1, 2],
                mailboxItemFlag: Flag.SEEN
            });
        });
        test("Synchronisly add flag to item in state", async () => {
            const adapted = MessageAdaptor.fromMailboxItem(
                messages.find(({ value: { flags } }) => !flags.includes(Flag.SEEN)),
                folder
            );
            store.commit(ADD_MESSAGES, { messages: [adapted] });
            const promise = store.dispatch(ADD_FLAG, {
                messages: adapted,
                flag: Flag.SEEN
            });
            expect(store.state[adapted.key].flags).toEqual(expect.arrayContaining([Flag.SEEN]));
            await promise;
            expect(store.state[adapted.key].flags).toEqual(expect.arrayContaining([Flag.SEEN]));
        });
        test("Only add flag to loaded items", () => {
            const adapted = createOnlyMetadata({ internalId: 1, folder });
            store.commit(ADD_MESSAGES, { messages: [adapted] });
            store.dispatch(ADD_FLAG, {
                messages: adapted,
                flag: Flag.SEEN
            });
            expect(store.state[adapted.key].flags).not.toEqual(expect.arrayContaining([Flag.SEEN]));
        });
        test("Do not add flag twice on message already having flag", () => {
            const adapted = MessageAdaptor.fromMailboxItem(
                messages.find(({ value: { flags } }) => flags.includes(Flag.SEEN)),
                folder
            );
            store.commit(ADD_MESSAGES, { messages: [adapted] });
            store.dispatch(ADD_FLAG, {
                messages: adapted,
                flag: Flag.SEEN
            });
            expect(store.state[adapted.key].flags.filter(flag => flag === Flag.SEEN).length).toEqual(1);
        });
        test("On failure remove flag ", async () => {
            const adapted = MessageAdaptor.fromMailboxItem(
                messages.find(({ value: { flags } }) => !flags.includes(Flag.SEEN)),
                folder
            );
            store.commit(ADD_MESSAGES, { messages: [adapted] });
            inject("MailboxItemsPersistence").addFlag.mockRejectedValueOnce("Failure");
            try {
                await store.dispatch(ADD_FLAG, {
                    messages: adapted,
                    flag: Flag.SEEN
                });
            } catch (e) {
                expect(e).toEqual("Failure");
            } finally {
                expect(store.state[adapted.key].flags).not.toEqual(expect.arrayContaining([Flag.SEEN]));
            }
        });
        test("if message has already the flag, dont call API", async () => {
            const adapted = MessageAdaptor.fromMailboxItem(
                messages.find(({ value: { flags } }) => flags.includes(Flag.SEEN)),
                folder
            );
            store.commit(ADD_MESSAGES, { messages: [adapted] });
            await store.dispatch(ADD_FLAG, { messages: adapted, flag: Flag.SEEN });
            expect(inject("MailboxItemsPersistence").addFlag).not.toHaveBeenCalled();
            expect(store.state[adapted.key].flags).toEqual(expect.arrayContaining([Flag.SEEN]));
        });
    });
    describe("REMOVE_FLAG", () => {
        test("Call relive flag remote API", async () => {
            const adapted = messages
                .filter(({ value: { flags } }) => flags.includes(Flag.SEEN))
                .slice(0, 5)
                .map(m => MessageAdaptor.fromMailboxItem(m, folder));
            store.commit(ADD_MESSAGES, { messages: adapted });
            await store.dispatch(DELETE_FLAG, {
                messages: adapted,
                flag: Flag.SEEN
            });
            const itemsId = adapted.map(message => message.remoteRef.internalId);
            expect(inject("MailboxItemsPersistence").deleteFlag).toHaveBeenCalledWith({
                itemsId,
                mailboxItemFlag: Flag.SEEN
            });
        });
        test("Call delete flag remote API only for message with [flag]", async () => {
            const adapted = messages
                .filter(({ value: { flags } }) => !flags.includes(Flag.SEEN))
                .slice(0, 5)
                .map(m => MessageAdaptor.fromMailboxItem(m, folder));
            store.commit(ADD_MESSAGES, { messages: adapted });
            await store.dispatch(DELETE_FLAG, {
                messages: adapted,
                flag: Flag.SEEN
            });
            expect(inject("MailboxItemsPersistence").deleteFlag).not.toHaveBeenCalled();
        });
        test("Call delete flag remote API for messages not yet loaded", async () => {
            const adapted = [1, 2].map(id => createOnlyMetadata({ internalId: id, folder }));
            store.commit(ADD_MESSAGES, { messages: adapted });
            await store.dispatch(DELETE_FLAG, {
                messages: adapted,
                flag: Flag.SEEN
            });
            expect(inject("MailboxItemsPersistence").deleteFlag).toHaveBeenCalledWith({
                itemsId: [1, 2],
                mailboxItemFlag: Flag.SEEN
            });
        });
        test("Synchronisly remove flag to item in state", async () => {
            const adapted = MessageAdaptor.fromMailboxItem(
                messages.find(({ value: { flags } }) => flags.includes(Flag.SEEN)),
                folder
            );
            store.commit(ADD_MESSAGES, { messages: [adapted] });
            const promise = store.dispatch(DELETE_FLAG, {
                messages: adapted,
                flag: Flag.SEEN
            });
            expect(store.state[adapted.key].flags).not.toEqual(expect.arrayContaining([Flag.SEEN]));
            await promise;
            expect(store.state[adapted.key].flags).not.toEqual(expect.arrayContaining([Flag.SEEN]));
        });

        test("On failure remove flag ", async () => {
            const adapted = MessageAdaptor.fromMailboxItem(
                messages.find(({ value: { flags } }) => flags.includes(Flag.SEEN)),
                folder
            );
            store.commit(ADD_MESSAGES, { messages: [adapted] });
            inject("MailboxItemsPersistence").deleteFlag.mockRejectedValueOnce("Failure");
            try {
                await store.dispatch(DELETE_FLAG, {
                    messages: adapted,
                    flag: Flag.SEEN
                });
            } catch (e) {
                expect(e).toEqual("Failure");
            } finally {
                expect(store.state[adapted.key].flags).toEqual(expect.arrayContaining([Flag.SEEN]));
            }
        });
        test("if message do not have the flag, dont call deleteFlag API", async () => {
            const adapted = MessageAdaptor.fromMailboxItem(
                messages.find(({ value: { flags } }) => !flags.includes(Flag.SEEN)),
                folder
            );
            store.commit(ADD_MESSAGES, { messages: [adapted] });
            await store.dispatch(DELETE_FLAG, { messages: adapted, flag: Flag.SEEN });
            expect(inject("MailboxItemsPersistence").deleteFlag).not.toHaveBeenCalled();
            expect(store.state[adapted.key].flags).not.toEqual(expect.arrayContaining([Flag.SEEN]));
        });
    });
    describe("FETCH_MESSAGE_METADATA", () => {
        test("Call fetch message API", () => {
            const adapted = [1, 2, 3].map(id => createOnlyMetadata({ internalId: id, folder }));
            store.commit(ADD_MESSAGES, { messages: adapted });
            store.dispatch(FETCH_MESSAGE_METADATA, { messages: adapted.map(m => m.key) });
            expect(inject("MailboxItemsPersistence").multipleGetById).toHaveBeenCalledWith([1, 2, 3]);
        });
        test("Call fetch message API is chunked", () => {
            const maxMultipleGetById = 500;
            const adapted = Array.from(Array(maxMultipleGetById * 4 + 2).keys()).map(id =>
                createWithMetadata({ internalId: id, folder })
            );
            store.commit(ADD_MESSAGES, { messages: adapted });
            store.dispatch(FETCH_MESSAGE_METADATA, { messages: adapted.map(m => m.key) });
            expect(inject("MailboxItemsPersistence").multipleGetById).toHaveBeenCalledTimes(5);
        });
        test("Add LOADING status while fetching to messages", async () => {
            const message = messages.pop();
            const adapted = createOnlyMetadata({ internalId: message.internalId, folder });
            store.commit(ADD_MESSAGES, { messages: [adapted] });
            inject("MailboxItemsPersistence").multipleGetById.mockResolvedValueOnce([message]);
            store.dispatch(FETCH_MESSAGE_METADATA, { messages: adapted.key });
            expect(store.state[adapted.key].loading).toEqual(LoadingStatus.LOADING);
        });
        test("Add LOADED status to messages", async () => {
            const message = messages.pop();
            const adapted = createOnlyMetadata({ internalId: message.internalId, folder });
            store.commit(ADD_MESSAGES, { messages: [adapted] });
            inject("MailboxItemsPersistence").multipleGetById.mockResolvedValueOnce([message]);
            await store.dispatch(FETCH_MESSAGE_METADATA, { messages: adapted.key });
            expect(store.state[adapted.key].loading).toEqual(LoadingStatus.LOADED);
        });
        test("Add ERROR status to messages not found", async () => {
            const message = messages.pop();
            const adapted = createOnlyMetadata({ internalId: message.internalId, folder });
            store.commit(ADD_MESSAGES, { messages: [adapted] });
            inject("MailboxItemsPersistence").multipleGetById.mockResolvedValueOnce([]);
            await store.dispatch(FETCH_MESSAGE_METADATA, { messages: adapted.key });
            expect(store.state[adapted.key].loading).toEqual(LoadingStatus.ERROR);
        });
    });
    describe("FETCH_MESSAGE_IF_NOT_LOADED", () => {
        test("Add message to state if not present", () => {
            const message = messages.pop();
            store.dispatch(FETCH_MESSAGE_IF_NOT_LOADED, { internalId: message.internalId, folder });
            const stored = Object.values(store.state);
            expect(stored.length).toEqual(1);
            const stub = stored.pop();

            expect(stub.remoteRef.internalId).toBe(message.internalId);
            expect(stub.loading).toBe(LoadingStatus.LOADING);
        });
        test("Keep message in state if already present", async () => {
            const message = messages.pop();
            const adapted = createOnlyMetadata({ internalId: message.internalId, folder });
            adapted.alreadyStored = true;
            store.commit(ADD_MESSAGES, { messages: [adapted] });
            store.dispatch(FETCH_MESSAGE_IF_NOT_LOADED, { internalId: message.internalId, folder });
            expect(store.state[adapted.key].alreadyStored).toBe(true);
        });
        test("Fetch message from remote if not already loaded", async () => {
            const message = messages.pop();
            inject("MailboxItemsPersistence").multipleGetById.mockResolvedValueOnce([message]);
            const adapted = await store.dispatch(FETCH_MESSAGE_IF_NOT_LOADED, {
                internalId: message.internalId,
                folder
            });
            expect(inject("MailboxItemsPersistence").multipleGetById).toBeCalledWith([message.internalId]);
            expect(store.state[adapted.key].loading).toEqual(LoadingStatus.LOADED);
            expect(store.state[adapted.key].subject).toEqual("testing");
        });
        test("Do not fetch message from remote if not already loaded", async () => {
            const message = messages.pop();
            const adapted = createOnlyMetadata({ internalId: message.internalId, folder });
            adapted.loading = LoadingStatus.LOADING;
            store.commit(ADD_MESSAGES, { messages: [adapted] });
            await store.dispatch(FETCH_MESSAGE_IF_NOT_LOADED, {
                internalId: message.internalId,
                folder
            });
            expect(inject("MailboxItemsPersistence").multipleGetById).not.toBeCalled();
        });
    });
    describe("REMOVE_MESSAGES", () => {
        test("Call remove message remote API", () => {
            const adapted = messages.slice(0, 5).map(m => MessageAdaptor.fromMailboxItem(m, folder));
            store.commit(ADD_MESSAGES, { messages: adapted });
            store.dispatch(REMOVE_MESSAGES, { messages: adapted });
            expect(inject("MailboxItemsPersistence").multipleDeleteById).toHaveBeenCalledWith(
                adapted.map(message => message.remoteRef.internalId)
            );
        });

        test("To synchronously mark messages as removed in state", () => {
            const adapted = MessageAdaptor.fromMailboxItem(messages[0], folder);
            store.commit(ADD_MESSAGES, { messages: [adapted] });
            store.dispatch(REMOVE_MESSAGES, { messages: adapted });
            expect(store.state[adapted.key]).toBeUndefined();
        });

        test("To remove message from store if api call is successfull", async () => {
            const adapted = MessageAdaptor.fromMailboxItem(messages[0], folder);
            store.commit(ADD_MESSAGES, { messages: [adapted] });
            expect(store.state[adapted.key]).toBeDefined();
            await store.dispatch(REMOVE_MESSAGES, { messages: adapted });
            expect(store.state[adapted.key]).toBeUndefined();
        });

        test("To restore old status if api call fail", async () => {
            let adapted = MessageAdaptor.fromMailboxItem(messages[0], folder);
            store.commit(ADD_MESSAGES, { messages: [adapted] });
            inject("MailboxItemsPersistence").multipleDeleteById.mockRejectedValueOnce("Failure");
            try {
                await store.dispatch(REMOVE_MESSAGES, { messages: adapted });
            } catch {
                // Nothing to do
            } finally {
                expect(store.state[adapted.key].status).toEqual(MessageStatus.IDLE);
            }
            adapted = createOnlyMetadata({ internalId: 1, folder });
            store.commit(ADD_MESSAGES, { messages: [adapted] });
            inject("MailboxItemsPersistence").multipleDeleteById.mockRejectedValueOnce("Failure");
            try {
                await store.dispatch(REMOVE_MESSAGES, { messages: adapted });
            } catch {
                // Nothing to do
            } finally {
                expect(store.state[adapted.key].status).toEqual(MessageStatus.IDLE);
            }
        });
    });
    describe("MOVE_MESSAGES", () => {
        const anotherFolder = { key: "folder-key2", remoteRef: { uid: "folder-key2" } };
        test("Call move message remote API", () => {
            const adapted = messages.slice(0, 5).map(m => MessageAdaptor.fromMailboxItem(m, folder));
            store.commit(ADD_MESSAGES, { messages: adapted });
            store.dispatch(MOVE_MESSAGES, { messages: adapted, folder: anotherFolder });
            expect(inject("ItemsTransferPersistence").move).toHaveBeenCalledWith(
                adapted.map(message => message.remoteRef.internalId)
            );
        });
        test("Not to move message from the same folder", async () => {
            const adapted = messages.slice(0, 5).map(m => MessageAdaptor.fromMailboxItem(m, anotherFolder));
            store.commit(ADD_MESSAGES, { messages: adapted });
            store.dispatch(MOVE_MESSAGES, { messages: adapted, folder: anotherFolder });
            expect(inject("ItemsTransferPersistence").move).not.toHaveBeenCalled();
        });
        test("To synchronously update messages in state", () => {
            const adapted = MessageAdaptor.fromMailboxItem(messages[0], folder);
            store.commit(ADD_MESSAGES, { messages: [adapted] });
            store.dispatch(MOVE_MESSAGES, { messages: adapted, folder: anotherFolder });
            expect(store.state[adapted.key].folderRef).toEqual(FolderAdaptor.toRef(anotherFolder));
        });

        test("To remove message from store if api call is successfull", async () => {
            const adapted = MessageAdaptor.fromMailboxItem(messages[0], folder);
            store.commit(ADD_MESSAGES, { messages: [adapted] });
            expect(store.state[adapted.key]).toBeDefined();
            await store.dispatch(MOVE_MESSAGES, { messages: adapted, folder: anotherFolder });
            expect(store.state[adapted.key].folderRef).toEqual(FolderAdaptor.toRef(anotherFolder));
        });

        test("To restore old status if api call fail", async () => {
            let adapted = MessageAdaptor.fromMailboxItem(messages[0], folder);
            store.commit(ADD_MESSAGES, { messages: [adapted] });
            inject("ItemsTransferPersistence").move.mockRejectedValueOnce("Failure");
            try {
                await store.dispatch(MOVE_MESSAGES, { messages: adapted, folder: anotherFolder });
            } catch {
                // Nothing to do
            } finally {
                expect(store.state[adapted.key].status).toEqual(MessageStatus.IDLE);
            }
            adapted = createOnlyMetadata({ internalId: 1, folder });
            store.commit(ADD_MESSAGES, { messages: [adapted] });
            inject("ItemsTransferPersistence").move.mockRejectedValueOnce("Failure");
            try {
                await store.dispatch(MOVE_MESSAGES, { messages: adapted, folder: anotherFolder });
            } catch {
                // Nothing to do
            } finally {
                expect(store.state[adapted.key].status).toEqual(MessageStatus.IDLE);
            }
        });
    });
    describe("MARK_MESSAGES_AS_", () => {
        test("MARK_MESSAGES_AS_READ", async () => {
            const flag = Flag.SEEN;
            const adapted = messages
                .filter(({ value: { flags } }) => !flags.includes(flag))
                .map(m => MessageAdaptor.fromMailboxItem(m, folder));
            store.commit(ADD_MESSAGES, { messages: adapted });
            await store.dispatch(MARK_MESSAGES_AS_READ, adapted);
            expect(Object.values(store.state).some(({ flags }) => !flags.includes(flag))).toBeFalsy();
        });
        test("MARK_MESSAGES_AS_UNREAD", async () => {
            const flag = Flag.SEEN;
            const adapted = messages
                .filter(({ value: { flags } }) => flags.includes(flag))
                .slice(0, 5)
                .map(m => MessageAdaptor.fromMailboxItem(m, folder));
            store.commit(ADD_MESSAGES, { messages: adapted });
            await store.dispatch(MARK_MESSAGES_AS_UNREAD, adapted);
            expect(Object.values(store.state).some(({ flags }) => flags.includes(flag))).toBeFalsy();
        });
        test("MARK_MESSAGES_AS_FLAGGED", async () => {
            const flag = Flag.FLAGGED;
            const adapted = messages
                .filter(({ value: { flags } }) => !flags.includes(flag))
                .slice(0, 5)
                .map(m => MessageAdaptor.fromMailboxItem(m, folder));
            store.commit(ADD_MESSAGES, { messages: adapted });
            await store.dispatch(MARK_MESSAGES_AS_FLAGGED, adapted);
            expect(Object.values(store.state).some(({ flags }) => !flags.includes(flag))).toBeFalsy();
        });
        test("MARK_MESSAGES_AS_UNFLAGGED", async () => {
            const flag = Flag.FLAGGED;
            const adapted = messages
                .filter(({ value: { flags } }) => flags.includes(flag))
                .slice(0, 5)
                .map(m => MessageAdaptor.fromMailboxItem(m, folder));
            store.commit(ADD_MESSAGES, { messages: adapted });
            await store.dispatch(MARK_MESSAGES_AS_UNFLAGGED, adapted);
            expect(Object.values(store.state).some(({ flags }) => flags.includes(flag))).toBeFalsy();
        });
    });
});
