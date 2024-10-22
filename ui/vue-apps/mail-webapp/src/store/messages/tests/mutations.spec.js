import mutations from "../mutations";
import { messageUtils, loadingStatusUtils } from "@bluemind/mail";

const { MessageStatus } = messageUtils;
const { LoadingStatus } = loadingStatusUtils;

describe("mutations", () => {
    describe("ADD_MESSAGES", () => {
        test("when state is empty", () => {
            const message = { key: "key1", subject: "mySubject" };
            const message2 = { key: "key2", subject: "anotherSubject" };
            const state = {};
            mutations.ADD_MESSAGES(state, { messages: [message, message2] });
            expect(state).toEqual({ [message.key]: message, [message2.key]: message2 });
        });

        test("overwrites message if it's already in state", () => {
            const message = { key: "key1", subject: "mySubject" };
            const state = { [message.key]: message };
            message.subject = "modified";
            mutations.ADD_MESSAGES(state, { messages: [message] });
            expect(state).toEqual({ [message.key]: message });
        });

        test("dont delete existing messages in state", () => {
            const message = { key: "key1", subject: "mySubject" };
            const state = { [message.key]: message };
            const message2 = { key: "key2", subject: "anotherSubject" };
            mutations.ADD_MESSAGES(state, { messages: [message2] });
            expect(state).toEqual({ [message.key]: message, [message2.key]: message2 });
        });
    });

    describe("REMOVE_MESSAGES", () => {
        test("dont delete other messages in state", () => {
            const message = { key: "key1", subject: "mySubject" };
            const message2 = { key: "key2", subject: "anotherSubject" };
            const state = { [message.key]: message, [message2.key]: message2 };
            mutations.REMOVE_MESSAGES(state, { messages: [message2] });
            expect(state).toEqual({ [message.key]: message });
        });

        test("do nothing if key dont exist", () => {
            const message = { key: "key1", subject: "mySubject" };
            const state = { [message.key]: message };
            mutations.REMOVE_MESSAGES(state, { messages: ["UNKNOWN-KEY"] });
            expect(state).toEqual({ [message.key]: message });
        });
    });

    describe("ADD_FLAG", () => {
        test("dont change other flag", () => {
            const message = { key: "key1", status: MessageStatus.IDLE, flags: ["OTHER"] };
            const state = { [message.key]: message };
            mutations.ADD_FLAG(state, { messages: [message], flag: "READ" });
            expect(state[message.key].flags).toEqual(["OTHER", "READ"]);
        });

        test("add flag to multiple messages", () => {
            const message = { key: "key1", status: MessageStatus.IDLE, flags: [] };
            const message2 = { key: "key2", status: MessageStatus.IDLE, flags: [] };
            const state = { [message.key]: message, [message2.key]: message2 };
            mutations.ADD_FLAG(state, { messages: [message, message2], flag: "READ" });
            expect(state[message.key].flags).toEqual(["READ"]);
            expect(state[message2.key].flags).toEqual(["READ"]);
        });
    });

    describe("DELETE_FLAG", () => {
        test("do nothing if flag is not set", () => {
            const message = { key: "key1", status: MessageStatus.IDLE, flags: ["OTHER"] };
            const state = { [message.key]: message };
            mutations.DELETE_FLAG(state, { messages: [message], flag: "READ" });
            expect(state[message.key].flags).toEqual(["OTHER"]);
        });

        test("dont change other flag", () => {
            const message = { key: "key1", status: MessageStatus.IDLE, flags: ["OTHER", "READ"] };
            const state = { [message.key]: message };
            mutations.DELETE_FLAG(state, { messages: [message], flag: "READ" });
            expect(state[message.key].flags).toEqual(["OTHER"]);
        });

        test("delete flag to multiple messages", () => {
            const message = { key: "key1", status: MessageStatus.IDLE, flags: ["READ"] };
            const message2 = { key: "key2", status: MessageStatus.IDLE, flags: ["READ"] };
            const state = { [message.key]: message, [message2.key]: message2 };
            mutations.DELETE_FLAG(state, { messages: [message, message2], flag: "READ" });
            expect(state[message.key].flags).toEqual([]);
            expect(state[message2.key].flags).toEqual([]);
        });
    });

    describe("SET_MESSAGES_STATUS", () => {
        test("can change status", () => {
            const message = { key: "key1", status: MessageStatus.SAVING };
            const state = { [message.key]: message };
            mutations.SET_MESSAGES_STATUS(state, [{ key: message.key, status: MessageStatus.IDLE }]);
            expect(state[message.key].status).toEqual(MessageStatus.IDLE);
        });
    });

    describe("SET_MESSAGES_LOADING_STATUS", () => {
        test("can change loading status", () => {
            const message = { key: "key1", loading: LoadingStatus.NOT_LOADED };
            const state = { [message.key]: message };
            mutations.SET_MESSAGES_LOADING_STATUS(state, [{ key: message.key, loading: LoadingStatus.LOADED }]);
            expect(state[message.key].loading).toEqual(LoadingStatus.LOADED);
        });
    });

    describe("MOVE_MESSAGES", () => {
        test("change message folderRef", () => {
            const message = { key: "key1", subject: "mySubject", folderRef: { key: 1 } };
            const message2 = { key: "key2", subject: "anotherSubject", folderRef: { key: 2 } };
            const state = { [message.key]: message, [message2.key]: message2 };
            mutations.MOVE_MESSAGES(state, { messages: [{ ...message2, folderRef: { key: 3 } }] });
            expect(state[message2.key].folderRef).toEqual({ key: 3 });
            expect(state[message.key].folderRef).toEqual({ key: 1 });
        });
    });

    describe("REMOVE_CONVERSATIONS", () => {
        test("change message folderRef", () => {
            const state = {};
            [({ key: "key1" }, { key: "key2" }, { key: "key3" }, { key: "key4" })].forEach(
                message => (state[message.key] = message)
            );
            mutations.REMOVE_CONVERSATIONS(state, [{ messages: ["key1", "key2"] }, { messages: ["key3"] }]);
            expect(state["key1"]).not.toBeDefined();
            expect(state["key2"]).not.toBeDefined();
            expect(state["key3"]).not.toBeDefined();
            expect(state["key4"]).toBeDefined();
        });
    });

    describe("SET_MESSAGE_FROM", () => {
        test("can change from", () => {
            const message = { key: "key1", from: "toto" };
            const state = { [message.key]: message };
            mutations.SET_MESSAGE_FROM(state, { messageKey: message.key, from: "blabla" });
            expect(state[message.key].from).toEqual("blabla");
        });
    });
    describe("ADD and REMOVE attachment", () => {
        const key = "key1";
        const attachment = {
            address: "1733A829-2AD8-4DA8-B185-E07DCA845A60",
            charset: "us-ascii",
            dispositionType: "ATTACHMENT",
            encoding: "base64",
            mime: "image/gif",
            size: 2934590
        };
        const alernativeStructure = {
            address: "TEXT",
            children: [{ mime: "text/plain" }, { mime: "text/html" }],
            mime: "multipart/alternative"
        };
        const mixedStructure = {
            children: [
                {
                    address: "TEXT",
                    children: [{ mime: "text/plain" }, { mime: "text/html" }],
                    mime: "multipart/alternative"
                },
                {
                    address: "add1",
                    charset: "us-ascii",
                    dispositionType: "ATTACHMENT",
                    encoding: "base64",
                    mime: "application/pdf",
                    size: 4
                },
                {
                    address: "add2",
                    charset: "us-ascii",
                    dispositionType: "ATTACHMENT",
                    encoding: "base64",
                    mime: "image/png",
                    size: 10
                }
            ],
            mime: "multipart/mixed"
        };
        describe("ADD_ATTACHMENT", () => {
            test("update message structure with a multipart/mixed and the attachment", () => {
                const key = "key1";
                const state = {
                    [key]: {
                        structure: alernativeStructure
                    }
                };
                mutations.ADD_ATTACHMENT(state, { messageKey: key, attachment });
                expect(state[key].structure).toMatchInlineSnapshot(`
                    Object {
                      "children": Array [
                        Object {
                          "address": "TEXT",
                          "children": Array [
                            Object {
                              "mime": "text/plain",
                            },
                            Object {
                              "mime": "text/html",
                            },
                          ],
                          "mime": "multipart/alternative",
                        },
                        Object {
                          "address": "1733A829-2AD8-4DA8-B185-E07DCA845A60",
                          "charset": "us-ascii",
                          "dispositionType": "ATTACHMENT",
                          "encoding": "base64",
                          "mime": "image/gif",
                          "size": 2934590,
                        },
                      ],
                      "mime": "multipart/mixed",
                    }
                `);
            });
            test("add the attachment the a multipart/mixed message", () => {
                const state = {
                    [key]: {
                        structure: mixedStructure
                    }
                };
                mutations.ADD_ATTACHMENT(state, { messageKey: key, attachment });
                expect(state[key].structure).toMatchInlineSnapshot(`
                    Object {
                      "children": Array [
                        Object {
                          "address": "TEXT",
                          "children": Array [
                            Object {
                              "mime": "text/plain",
                            },
                            Object {
                              "mime": "text/html",
                            },
                          ],
                          "mime": "multipart/alternative",
                        },
                        Object {
                          "address": "add1",
                          "charset": "us-ascii",
                          "dispositionType": "ATTACHMENT",
                          "encoding": "base64",
                          "mime": "application/pdf",
                          "size": 4,
                        },
                        Object {
                          "address": "add2",
                          "charset": "us-ascii",
                          "dispositionType": "ATTACHMENT",
                          "encoding": "base64",
                          "mime": "image/png",
                          "size": 10,
                        },
                        Object {
                          "address": "1733A829-2AD8-4DA8-B185-E07DCA845A60",
                          "charset": "us-ascii",
                          "dispositionType": "ATTACHMENT",
                          "encoding": "base64",
                          "mime": "image/gif",
                          "size": 2934590,
                        },
                      ],
                      "mime": "multipart/mixed",
                    }
                `);
            });
        });
        describe("REMOVE_ATTACHMENT", () => {
            test("remove an attachment updates the message structure", () => {
                const state = {
                    [key]: { structure: mixedStructure }
                };
                mutations.REMOVE_ATTACHMENT(state, { messageKey: key, address: "add1" });
                expect(state[key].structure).toMatchInlineSnapshot(`
                    Object {
                      "children": Array [
                        Object {
                          "address": "TEXT",
                          "children": Array [
                            Object {
                              "mime": "text/plain",
                            },
                            Object {
                              "mime": "text/html",
                            },
                          ],
                          "mime": "multipart/alternative",
                        },
                        Object {
                          "address": "add2",
                          "charset": "us-ascii",
                          "dispositionType": "ATTACHMENT",
                          "encoding": "base64",
                          "mime": "image/png",
                          "size": 10,
                        },
                      ],
                      "mime": "multipart/mixed",
                    }
                `);
            });
            test("remove all attachments should remove the multipart/mixed part", () => {
                const state = {
                    [key]: { structure: mixedStructure }
                };
                mutations.REMOVE_ATTACHMENT(state, { messageKey: key, address: "add1" });
                mutations.REMOVE_ATTACHMENT(state, { messageKey: key, address: "add2" });

                expect(state[key].structure).toMatchInlineSnapshot(`
                                  Object {
                                    "address": "TEXT",
                                    "children": Array [
                                      Object {
                                        "mime": "text/plain",
                                      },
                                      Object {
                                        "mime": "text/html",
                                      },
                                    ],
                                    "mime": "multipart/alternative",
                                  }
                              `);
            });
            test("remove a non existing attachment does not change the message structure", () => {
                const state = {
                    [key]: { structure: alernativeStructure }
                };
                mutations.REMOVE_ATTACHMENT(state, { messageKey: key, address: "add2" });
                expect(state[key].structure).toEqual(alernativeStructure);
            });
        });
    });
});
