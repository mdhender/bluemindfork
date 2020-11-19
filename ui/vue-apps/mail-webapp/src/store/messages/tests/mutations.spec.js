import mutations from "../mutations";
import { MessageStatus } from "../../../model/message";
import { Flag } from "@bluemind/email";

describe("mutations", () => {
    describe("ADD_MESSAGES", () => {
        test("when state is empty", () => {
            const message = { key: "key1", subject: "mySubject" };
            const message2 = { key: "key2", subject: "anotherSubject" };
            const state = {};
            mutations.ADD_MESSAGES(state, [message, message2]);
            expect(state).toEqual({ [message.key]: message, [message2.key]: message2 });
        });

        test("overwrites message if it's already in state", () => {
            const message = { key: "key1", subject: "mySubject" };
            const state = { [message.key]: message };
            message.subject = "modified";
            mutations.ADD_MESSAGES(state, [message]);
            expect(state).toEqual({ [message.key]: message });
        });

        test("dont delete existing messages in state", () => {
            const message = { key: "key1", subject: "mySubject" };
            const state = { [message.key]: message };
            const message2 = { key: "key2", subject: "anotherSubject" };
            mutations.ADD_MESSAGES(state, [message2]);
            expect(state).toEqual({ [message.key]: message, [message2.key]: message2 });
        });
    });

    describe("REMOVE_MESSAGES", () => {
        test("dont delete other messages in state", () => {
            const message = { key: "key1", subject: "mySubject" };
            const message2 = { key: "key2", subject: "anotherSubject" };
            const state = { [message.key]: message, [message2.key]: message2 };
            mutations.REMOVE_MESSAGES(state, [message2]);
            expect(state).toEqual({ [message.key]: message });
        });

        test("do nothing if key dont exist", () => {
            const message = { key: "key1", subject: "mySubject" };
            const state = { [message.key]: message };
            mutations.REMOVE_MESSAGES(state, ["UNKNOWN-KEY"]);
            expect(state).toEqual({ [message.key]: message });
        });
    });

    describe("ADD_FLAG", () => {
        test("dont change other flag", () => {
            const message = { key: "key1", status: MessageStatus.LOADED, flags: ["OTHER"] };
            const state = { [message.key]: message };
            mutations.ADD_FLAG(state, { messages: [message], flag: "READ" });
            expect(state[message.key].flags).toEqual(["OTHER", "READ"]);
        });

        test("add flag to multiple messages", () => {
            const message = { key: "key1", status: MessageStatus.LOADED, flags: [] };
            const message2 = { key: "key2", status: MessageStatus.LOADED, flags: [] };
            const state = { [message.key]: message, [message2.key]: message2 };
            mutations.ADD_FLAG(state, { messages: [message, message2], flag: "READ" });
            expect(state[message.key].flags).toEqual(["READ"]);
            expect(state[message2.key].flags).toEqual(["READ"]);
        });
    });

    describe("DELETE_FLAG", () => {
        test("do nothing if flag is not set", () => {
            const message = { key: "key1", status: MessageStatus.LOADED, flags: ["OTHER"] };
            const state = { [message.key]: message };
            mutations.DELETE_FLAG(state, { messages: [message], flag: "READ" });
            expect(state[message.key].flags).toEqual(["OTHER"]);
        });

        test("dont change other flag", () => {
            const message = { key: "key1", status: MessageStatus.LOADED, flags: ["OTHER", "READ"] };
            const state = { [message.key]: message };
            mutations.DELETE_FLAG(state, { messages: [message], flag: "READ" });
            expect(state[message.key].flags).toEqual(["OTHER"]);
        });

        test("delete flag to multiple messages", () => {
            const message = { key: "key1", status: MessageStatus.LOADED, flags: ["READ"] };
            const message2 = { key: "key2", status: MessageStatus.LOADED, flags: ["READ"] };
            const state = { [message.key]: message, [message2.key]: message2 };
            mutations.DELETE_FLAG(state, { messages: [message, message2], flag: "READ" });
            expect(state[message.key].flags).toEqual([]);
            expect(state[message2.key].flags).toEqual([]);
        });
    });

    describe("SET_MESSAGES_STATUS", () => {
        test("can change status", () => {
            const message = { key: "key1", status: MessageStatus.NOT_LOADED };
            const state = { [message.key]: message };
            mutations.SET_MESSAGES_STATUS(state, [{ key: message.key, status: MessageStatus.LOADED }]);
            expect(state[message.key].status).toEqual(MessageStatus.LOADED);
        });
    });

    describe("MOVE_MESSAGES", () => {
        test("dont delete other messages in state", () => {
            const message = { key: "key1", subject: "mySubject" };
            const message2 = { key: "key2", subject: "anotherSubject" };
            const state = { [message.key]: message, [message2.key]: message2 };
            mutations.MOVE_MESSAGES(state, { messages: [message2] });
            expect(state).toEqual({ [message.key]: message });
        });

        test("do nothing if key dont exist", () => {
            const message = { key: "key1", subject: "mySubject" };
            const state = { [message.key]: message };
            mutations.MOVE_MESSAGES(state, { messages: ["UNKNOWN-KEY"] });
            expect(state).toEqual({ [message.key]: message });
        });
    });

    describe("SET_UNREAD_COUNT", () => {
        test("Mark message from folder as read if unread count is 0", () => {
            const message = {
                key: "key1",
                subject: "mySubject",
                folderRef: { key: "folder" },
                flags: [],
                status: MessageStatus.LOADED
            };
            const state = { [message.key]: message };
            mutations.SET_UNREAD_COUNT(state, { key: "folder", unread: 0 });
            expect(state[message.key].flags).toEqual([Flag.SEEN]);
        });
        test("do not mark message from other folder", () => {
            const message = {
                key: "key1",
                subject: "mySubject",
                folderRef: { key: "another folder" },
                flags: [],
                status: MessageStatus.LOADED
            };
            const state = { [message.key]: message };
            mutations.SET_UNREAD_COUNT(state, { key: "folder", unread: 0 });
            expect(state[message.key].flags).toEqual([]);
        });
        test("do not mark message not loaded", () => {
            const message = { key: "key1", subject: "mySubject", folderRef: { key: "folder" }, flags: [] };
            const state = { [message.key]: message };
            mutations.SET_UNREAD_COUNT(state, { key: "folder", unread: 0 });
            expect(state[message.key].flags).toEqual([]);
        });
        test("do nothing if unread is not equal to 0", () => {
            const message = { key: "key1", subject: "mySubject", folderRef: { key: "folder" }, flags: [] };
            const state = { [message.key]: message };
            mutations.SET_UNREAD_COUNT(state, { key: "folder", unread: 10 });
            expect(state[message.key].flags).toEqual([]);
        });
    });
});
