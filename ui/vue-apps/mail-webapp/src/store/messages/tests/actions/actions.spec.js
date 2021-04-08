import Vue from "vue";
import Vuex from "vuex";
import cloneDeep from "lodash.clonedeep";

import { Flag } from "@bluemind/email";
import ServiceLocator, { inject } from "@bluemind/inject";
import { MockMailboxItemsClient, MockMailboxFoldersClient, MockItemsTransferClient } from "@bluemind/test-utils";

import messageStore from "../../index";
import MessageAdaptor from "../../helpers/MessageAdaptor";
import { MessageStatus, createOnlyMetadata } from "~model/message";
import { ADD_MESSAGES } from "~mutations";
import {
    ADD_FLAG,
    DELETE_FLAG,
    EMPTY_FOLDER,
    FETCH_MESSAGE_METADATA,
    REMOVE_MESSAGES,
    MOVE_MESSAGES,
    MARK_MESSAGES_AS_FLAGGED,
    MARK_MESSAGES_AS_READ,
    MARK_MESSAGES_AS_UNFLAGGED,
    MARK_MESSAGES_AS_UNREAD
} from "~actions";
import { LoadingStatus } from "../../../../model/loading-status";
import { FETCH_MESSAGE_IF_NOT_LOADED } from "../../../types/actions";

Vue.use(Vuex);

describe("Messages actions", () => {
    let store;
    let folder = { key: "folder-key", remoteRef: { uid: "folder-key" } };
    let messages;
    const mailbox = {
        type: "",
        name: "",
        remoteRef: {}
    };

    beforeEach(() => {
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
            store.commit(ADD_MESSAGES, adapted);
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
            store.commit(ADD_MESSAGES, adapted);
            await store.dispatch(ADD_FLAG, {
                messages: adapted,
                flag: Flag.SEEN
            });
            expect(inject("MailboxItemsPersistence").addFlag).not.toHaveBeenCalled();
        });
        test("Call add flag remote API for messages not yet loaded", async () => {
            const adapted = [1, 2].map(id => createOnlyMetadata({ internalId: id, folder }));
            store.commit(ADD_MESSAGES, adapted);
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
            store.commit(ADD_MESSAGES, [adapted]);
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
            store.commit(ADD_MESSAGES, [adapted]);
            store.dispatch(ADD_FLAG, {
                message: adapted,
                flag: Flag.SEEN
            });
            expect(store.state[adapted.key].flags).not.toEqual(expect.arrayContaining([Flag.SEEN]));
        });
        test("Do not add flag twice on message already having flag", () => {
            const adapted = MessageAdaptor.fromMailboxItem(
                messages.find(({ value: { flags } }) => flags.includes(Flag.SEEN)),
                folder
            );
            store.commit(ADD_MESSAGES, [adapted]);
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
            store.commit(ADD_MESSAGES, [adapted]);
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
        test("On failure do not remove flag for message already flagged ", async () => {
            const adapted = MessageAdaptor.fromMailboxItem(
                messages.find(({ value: { flags } }) => flags.includes(Flag.SEEN)),
                folder
            );
            store.commit(ADD_MESSAGES, [adapted]);
            inject("MailboxItemsPersistence").addFlag.mockRejectedValueOnce("Failure");
            try {
                await store.dispatch(ADD_FLAG, {
                    messages: adapted,
                    flag: Flag.SEEN
                });
            } finally {
                expect(store.state[adapted.key].flags).toEqual(expect.arrayContaining([Flag.SEEN]));
            }
        });
    });
    describe("REMOVE_FLAG", () => {
        test("Call relive flag remote API", async () => {
            const adapted = messages
                .filter(({ value: { flags } }) => flags.includes(Flag.SEEN))
                .slice(0, 5)
                .map(m => MessageAdaptor.fromMailboxItem(m, folder));
            store.commit(ADD_MESSAGES, adapted);
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
            store.commit(ADD_MESSAGES, adapted);
            await store.dispatch(DELETE_FLAG, {
                messages: adapted,
                flag: Flag.SEEN
            });
            expect(inject("MailboxItemsPersistence").deleteFlag).not.toHaveBeenCalled();
        });
        test("Call delete flag remote API for messages not yet loaded", async () => {
            const adapted = [1, 2].map(id => createOnlyMetadata({ internalId: id, folder }));
            store.commit(ADD_MESSAGES, adapted);
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
            store.commit(ADD_MESSAGES, [adapted]);
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
            store.commit(ADD_MESSAGES, [adapted]);
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
        test("On failure do not re-add flag for unflagged messages ", async () => {
            const adapted = MessageAdaptor.fromMailboxItem(
                messages.find(({ value: { flags } }) => !flags.includes(Flag.SEEN)),
                folder
            );
            store.commit(ADD_MESSAGES, [adapted]);
            inject("MailboxItemsPersistence").deleteFlag.mockRejectedValueOnce("Failure");
            try {
                await store.dispatch(DELETE_FLAG, {
                    messages: adapted,
                    flag: Flag.SEEN
                });
            } finally {
                expect(store.state[adapted.key].flags).not.toEqual(expect.arrayContaining([Flag.SEEN]));
            }
        });
    });
    describe("FETCH_MESSAGE_METADATA", () => {
        test("Call fetch message API", () => {
            const adapted = [1, 2, 3].map(id => createOnlyMetadata({ internalId: id, folder }));
            store.commit(ADD_MESSAGES, adapted);
            store.dispatch(FETCH_MESSAGE_METADATA, adapted);
            expect(inject("MailboxItemsPersistence").multipleById).toHaveBeenCalledWith([1, 2, 3]);
        });
        test("Add LOADING status while fetching to messages", async () => {
            const message = messages.pop();
            const adapted = createOnlyMetadata({ internalId: message.internalId, folder });
            store.commit(ADD_MESSAGES, [adapted]);
            inject("MailboxItemsPersistence").multipleById.mockResolvedValueOnce([message]);
            store.dispatch(FETCH_MESSAGE_METADATA, adapted);
            expect(store.state[adapted.key].loading).toEqual(LoadingStatus.LOADING);
        });
        test("Add LOADED status to messages", async () => {
            const message = messages.pop();
            const adapted = createOnlyMetadata({ internalId: message.internalId, folder });
            store.commit(ADD_MESSAGES, [adapted]);
            inject("MailboxItemsPersistence").multipleById.mockResolvedValueOnce([message]);
            await store.dispatch(FETCH_MESSAGE_METADATA, adapted);
            expect(store.state[adapted.key].loading).toEqual(LoadingStatus.LOADED);
        });
        test("Add ERROR status to messages not found", async () => {
            const message = messages.pop();
            const adapted = createOnlyMetadata({ internalId: message.internalId, folder });
            store.commit(ADD_MESSAGES, [adapted]);
            inject("MailboxItemsPersistence").multipleById.mockResolvedValueOnce([]);
            await store.dispatch(FETCH_MESSAGE_METADATA, adapted);
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
            adapted.alreadStored = true;
            store.commit(ADD_MESSAGES, [adapted]);
            store.dispatch(FETCH_MESSAGE_METADATA, adapted);
            expect(store.state[adapted.key].alreadStored).toBeTruthy();
        });
        test("Fetch message from remote if not already loaded", async () => {
            const message = messages.pop();
            inject("MailboxItemsPersistence").multipleById.mockResolvedValueOnce([message]);
            const adapted = await store.dispatch(FETCH_MESSAGE_IF_NOT_LOADED, {
                internalId: message.internalId,
                folder
            });
            expect(inject("MailboxItemsPersistence").multipleById).toBeCalledWith([message.internalId]);
            expect(store.state[adapted.key].loading).toEqual(LoadingStatus.LOADED);
            expect(store.state[adapted.key].subject).toEqual("testing");
        });
        test("Do not fetch message from remote if not already loaded", async () => {
            const message = messages.pop();
            const adapted = createOnlyMetadata({ internalId: message.internalId, folder });
            adapted.loading = LoadingStatus.LOADING;
            store.commit(ADD_MESSAGES, [adapted]);
            await store.dispatch(FETCH_MESSAGE_IF_NOT_LOADED, {
                internalId: message.internalId,
                folder
            });
            expect(inject("MailboxItemsPersistence").multipleById).not.toBeCalled();
        });
    });
    describe("REMOVE_MESSAGES", () => {
        test("Call remove message remote API", () => {
            const adapted = messages.slice(0, 5).map(m => MessageAdaptor.fromMailboxItem(m, folder));
            store.commit(ADD_MESSAGES, adapted);
            store.dispatch(REMOVE_MESSAGES, adapted);
            expect(inject("MailboxItemsPersistence").multipleDeleteById).toHaveBeenCalledWith(
                adapted.map(message => message.remoteRef.internalId)
            );
        });

        test("To synchronously mark messages as removed in state", () => {
            const adapted = MessageAdaptor.fromMailboxItem(messages[0], folder);
            store.commit(ADD_MESSAGES, [adapted]);
            store.dispatch(REMOVE_MESSAGES, adapted);
            expect(store.state[adapted.key].status).toEqual(MessageStatus.REMOVED);
        });

        test("To remove message from store if api call is successfull", async () => {
            const adapted = MessageAdaptor.fromMailboxItem(messages[0], folder);
            store.commit(ADD_MESSAGES, [adapted]);
            expect(store.state[adapted.key]).toBeDefined();
            await store.dispatch(REMOVE_MESSAGES, adapted);
            expect(store.state[adapted.key]).toBeUndefined();
        });

        test("To restore old status if api call fail", async () => {
            let adapted = MessageAdaptor.fromMailboxItem(messages[0], folder);
            store.commit(ADD_MESSAGES, [adapted]);
            inject("MailboxItemsPersistence").multipleDeleteById.mockRejectedValueOnce("Failure");
            try {
                await store.dispatch(REMOVE_MESSAGES, adapted);
            } catch {
                // Nothing to do
            } finally {
                expect(store.state[adapted.key].status).toEqual(MessageStatus.IDLE);
            }
            adapted = createOnlyMetadata({ internalId: 1, folder });
            store.commit(ADD_MESSAGES, [adapted]);
            inject("MailboxItemsPersistence").multipleDeleteById.mockRejectedValueOnce("Failure");
            try {
                await store.dispatch(REMOVE_MESSAGES, adapted);
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
            store.commit(ADD_MESSAGES, adapted);
            store.dispatch(MOVE_MESSAGES, { messages: adapted, folder: anotherFolder });
            expect(inject("ItemsTransferPersistence").move).toHaveBeenCalledWith(
                adapted.map(message => message.remoteRef.internalId)
            );
        });
        test("Not to move message from the same folder", async () => {
            const adapted = messages.slice(0, 5).map(m => MessageAdaptor.fromMailboxItem(m, anotherFolder));
            store.commit(ADD_MESSAGES, adapted);
            store.dispatch(MOVE_MESSAGES, { messages: adapted, folder: anotherFolder });
            expect(inject("ItemsTransferPersistence").move).not.toHaveBeenCalled();
        });
        test("To synchronously mark messages as removed in state", () => {
            const adapted = MessageAdaptor.fromMailboxItem(messages[0], folder);
            store.commit(ADD_MESSAGES, [adapted]);
            store.dispatch(MOVE_MESSAGES, { messages: adapted, folder: anotherFolder });
            expect(store.state[adapted.key].status).toEqual(MessageStatus.REMOVED);
        });

        test("To remove message from store if api call is successfull", async () => {
            const adapted = MessageAdaptor.fromMailboxItem(messages[0], folder);
            store.commit(ADD_MESSAGES, [adapted]);
            expect(store.state[adapted.key]).toBeDefined();
            await store.dispatch(MOVE_MESSAGES, { messages: adapted, folder: anotherFolder });
            expect(store.state[adapted.key]).toBeUndefined();
        });

        test("To restore old status if api call fail", async () => {
            let adapted = MessageAdaptor.fromMailboxItem(messages[0], folder);
            store.commit(ADD_MESSAGES, [adapted]);
            inject("ItemsTransferPersistence").move.mockRejectedValueOnce("Failure");
            try {
                await store.dispatch(MOVE_MESSAGES, { messages: adapted, folder: anotherFolder });
            } catch {
                // Nothing to do
            } finally {
                expect(store.state[adapted.key].status).toEqual(MessageStatus.IDLE);
            }
            adapted = createOnlyMetadata({ internalId: 1, folder });
            store.commit(ADD_MESSAGES, [adapted]);
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
            store.commit(ADD_MESSAGES, adapted);
            await store.dispatch(MARK_MESSAGES_AS_READ, adapted);
            expect(Object.values(store.state).some(({ flags }) => !flags.includes(flag))).toBeFalsy();
        });
        test("MARK_MESSAGES_AS_UNREAD", async () => {
            const flag = Flag.SEEN;
            const adapted = messages
                .filter(({ value: { flags } }) => flags.includes(flag))
                .slice(0, 5)
                .map(m => MessageAdaptor.fromMailboxItem(m, folder));
            store.commit(ADD_MESSAGES, adapted);
            await store.dispatch(MARK_MESSAGES_AS_UNREAD, adapted);
            expect(Object.values(store.state).some(({ flags }) => flags.includes(flag))).toBeFalsy();
        });
        test("MARK_MESSAGES_AS_FLAGGED", async () => {
            const flag = Flag.FLAGGED;
            const adapted = messages
                .filter(({ value: { flags } }) => !flags.includes(flag))
                .slice(0, 5)
                .map(m => MessageAdaptor.fromMailboxItem(m, folder));
            store.commit(ADD_MESSAGES, adapted);
            await store.dispatch(MARK_MESSAGES_AS_FLAGGED, adapted);
            expect(Object.values(store.state).some(({ flags }) => !flags.includes(flag))).toBeFalsy();
        });
        test("MARK_MESSAGES_AS_UNFLAGGED", async () => {
            const flag = Flag.FLAGGED;
            const adapted = messages
                .filter(({ value: { flags } }) => flags.includes(flag))
                .slice(0, 5)
                .map(m => MessageAdaptor.fromMailboxItem(m, folder));
            store.commit(ADD_MESSAGES, adapted);
            await store.dispatch(MARK_MESSAGES_AS_UNFLAGGED, adapted);
            expect(Object.values(store.state).some(({ flags }) => flags.includes(flag))).toBeFalsy();
        });
    });
    describe("EMPTY_FOLDER", () => {
        test("Call remote API", async () => {
            const adapted = messages.slice(0, 5).map(m => MessageAdaptor.fromMailboxItem(m, folder));
            store.commit(ADD_MESSAGES, adapted);
            store.dispatch(EMPTY_FOLDER, { folder, mailbox });
            expect(inject("MailboxFoldersPersistence").removeMessages).toHaveBeenCalledWith(
                folder.remoteRef.internalId
            );
        });
        test("Flag messages as removed while emptying", () => {
            const adapted = MessageAdaptor.fromMailboxItem(messages[0], folder);
            store.commit(ADD_MESSAGES, [adapted]);
            store.dispatch(EMPTY_FOLDER, { folder, mailbox });
            expect(store.state[adapted.key].status).toEqual(MessageStatus.REMOVED);
        });
        test("Remove messages from store after the remote call", async () => {
            const adapted = MessageAdaptor.fromMailboxItem(messages[0], folder);
            store.commit(ADD_MESSAGES, [adapted]);
            expect(store.state[adapted.key]).toBeDefined();
            await store.dispatch(EMPTY_FOLDER, { folder, mailbox });
            expect(store.state[adapted.key]).toBeUndefined();
        });
        test("Restore messages status if API call fails", async () => {
            let adapted = MessageAdaptor.fromMailboxItem(messages[0], folder);
            store.commit(ADD_MESSAGES, [adapted]);
            inject("MailboxFoldersPersistence").removeMessages.mockRejectedValueOnce("Failure");
            try {
                await store.dispatch(EMPTY_FOLDER, { folder, mailbox });
            } catch {
                // Nothing to do
            } finally {
                expect(store.state[adapted.key].status).toEqual(MessageStatus.IDLE);
            }
        });
    });
});
