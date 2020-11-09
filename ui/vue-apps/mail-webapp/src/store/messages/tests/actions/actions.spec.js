import Vue from "vue";
import Vuex from "vuex";
import cloneDeep from "lodash.clonedeep";

import { Flag } from "@bluemind/email";
import ServiceLocator, { inject } from "@bluemind/inject";
import { MockMailboxItemsClient } from "@bluemind/test-utils";

import messageStore from "../../index";
import MessageAdaptor from "../../helpers/MessageAdaptor";
import { MessageStatus, createOnlyMetadata } from "../../../../model/message";
import { ADD_MESSAGES } from "~mutations";
import { ADD_FLAG, DELETE_FLAG, FETCH_MESSAGE_METADATA, REMOVE_MESSAGES } from "~actions";

Vue.use(Vuex);

describe("Messages actions", () => {
    let store;
    let folder = { key: "folder-key", uid: "folder-key" };
    let messages;

    beforeEach(() => {
        store = new Vuex.Store(cloneDeep(messageStore));
        ServiceLocator.register({ provide: "MailboxItemsPersistence", use: new MockMailboxItemsClient(messages) });
        messages = cloneDeep(require("../../../tests/data/users/alice/messages.json"));
    });

    describe("ADD_FLAG", () => {
        test("Call add flag remote API", async () => {
            const adapted = messages
                .filter(({ value: { flags } }) => !flags.includes(Flag.SEEN))
                .slice(0, 5)
                .map(m => MessageAdaptor.fromMailboxItem(m, folder));
            store.commit(ADD_MESSAGES, adapted);
            const messageKeys = adapted.map(message => message.key);
            await store.dispatch(ADD_FLAG, {
                messageKeys,
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
            const messageKeys = adapted.map(message => message.key);
            await store.dispatch(ADD_FLAG, {
                messageKeys,
                flag: Flag.SEEN
            });
            expect(inject("MailboxItemsPersistence").addFlag).not.toHaveBeenCalled();
        });
        test("Call add flag remote API for messages not yet loaded", async () => {
            const adapted = [1, 2].map(id => createOnlyMetadata({ internalId: id, folder }));
            store.commit(ADD_MESSAGES, adapted);
            const messageKeys = adapted.map(message => message.key);
            await store.dispatch(ADD_FLAG, {
                messageKeys,
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
                messageKeys: [adapted.key],
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
                messageKeys: [1],
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
                messageKeys: [adapted.key],
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
                    messageKeys: [adapted.key],
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
                    messageKeys: [adapted.key],
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
            const messageKeys = adapted.map(message => message.key);
            await store.dispatch(DELETE_FLAG, {
                messageKeys,
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
            const messageKeys = adapted.map(message => message.key);
            await store.dispatch(DELETE_FLAG, {
                messageKeys,
                flag: Flag.SEEN
            });
            expect(inject("MailboxItemsPersistence").deleteFlag).not.toHaveBeenCalled();
        });
        test("Call delete flag remote API for messages not yet loaded", async () => {
            const adapted = [1, 2].map(id => createOnlyMetadata({ internalId: id, folder }));
            store.commit(ADD_MESSAGES, adapted);
            const messageKeys = adapted.map(message => message.key);
            await store.dispatch(DELETE_FLAG, {
                messageKeys,
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
                messageKeys: [adapted.key],
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
                    messageKeys: [adapted.key],
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
                    messageKeys: [adapted.key],
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
            store.dispatch(FETCH_MESSAGE_METADATA, {
                messageKeys: adapted.map(message => message.key)
            });
            expect(inject("MailboxItemsPersistence").multipleById).toHaveBeenCalledWith([1, 2, 3]);
        });
        test("Add LOADED status to messages", async () => {
            const message = messages.pop();
            const adapted = createOnlyMetadata({ internalId: message.internalId, folder });
            store.commit(ADD_MESSAGES, [adapted]);
            inject("MailboxItemsPersistence").multipleById.mockResolvedValueOnce([message]);
            await store.dispatch(FETCH_MESSAGE_METADATA, {
                messageKeys: [adapted.key]
            });
            expect(store.state[adapted.key].status).toEqual(MessageStatus.LOADED);
        });
    });
    describe("REMOVE_MESSAGES", () => {
        test("Call remove message remote API", () => {
            const adapted = messages.slice(0, 5).map(m => MessageAdaptor.fromMailboxItem(m, folder));
            store.commit(ADD_MESSAGES, adapted);
            store.dispatch(
                REMOVE_MESSAGES,
                adapted.map(message => message.key)
            );
            expect(inject("MailboxItemsPersistence").multipleDeleteById).toHaveBeenCalledWith(
                adapted.map(message => message.remoteRef.internalId)
            );
        });

        test("To synchronously mark messages as removed in state", () => {
            const adapted = MessageAdaptor.fromMailboxItem(messages[0], folder);
            store.commit(ADD_MESSAGES, [adapted]);
            store.dispatch(REMOVE_MESSAGES, adapted.key);
            expect(store.state[adapted.key].status).toEqual(MessageStatus.REMOVED);
        });

        test("To remove message from store if api call is successfull", async () => {
            const adapted = MessageAdaptor.fromMailboxItem(messages[0], folder);
            store.commit(ADD_MESSAGES, [adapted]);
            expect(store.state[adapted.key]).toBeDefined();
            await store.dispatch(REMOVE_MESSAGES, adapted.key);
            expect(store.state[adapted.key]).toBeUndefined();
        });

        test("To restore old status if api call fail", async () => {
            let adapted = MessageAdaptor.fromMailboxItem(messages[0], folder);
            store.commit(ADD_MESSAGES, [adapted]);
            inject("MailboxItemsPersistence").multipleDeleteById.mockRejectedValueOnce("Failure");
            try {
                await store.dispatch(REMOVE_MESSAGES, adapted.key);
            } catch {
                // Nothing to do
            } finally {
                expect(store.state[adapted.key].status).toEqual(MessageStatus.LOADED);
            }
            adapted = createOnlyMetadata({ internalId: 1, folder });
            store.commit(ADD_MESSAGES, [adapted]);
            inject("MailboxItemsPersistence").multipleDeleteById.mockRejectedValueOnce("Failure");
            try {
                await store.dispatch(REMOVE_MESSAGES, adapted.key);
            } catch {
                // Nothing to do
            } finally {
                expect(store.state[adapted.key].status).toEqual(MessageStatus.NOT_LOADED);
            }
        });
    });
});
