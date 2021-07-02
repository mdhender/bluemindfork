const { create, rename } = require("../folder");
const { MailboxType } = require("../mailbox");
import injector from "@bluemind/inject";

injector.register({ provide: "i18n", use: { t: n => n } });
describe("Folder model functions", () => {
    describe("create", () => {
        test("root folder inside a user mailbox", () => {
            const mailbox = { writable: true, root: "", remoteRef: {}, key: "mailbox-key", type: MailboxType.USER };
            expect(create("123", "name", null, mailbox)).toMatchInlineSnapshot(`
                Object {
                  "allowSubfolder": true,
                  "default": false,
                  "expanded": false,
                  "imapName": "name",
                  "key": "123",
                  "mailboxRef": Object {
                    "key": "mailbox-key",
                    "uid": undefined,
                  },
                  "name": "name",
                  "parent": null,
                  "path": "name",
                  "remoteRef": Object {
                    "internalId": null,
                    "uid": null,
                  },
                  "unread": 0,
                  "writable": true,
                }
            `);
        });
        test("sub folder inside a user mailbox", () => {
            const mailbox = { writable: true, root: "", remoteRef: {}, key: "mailbox-key", type: MailboxType.USER };
            expect(create("123", "name", { key: "0", path: "parent/folder", uid: "0" }, mailbox))
                .toMatchInlineSnapshot(`
                Object {
                  "allowSubfolder": true,
                  "default": false,
                  "expanded": false,
                  "imapName": "name",
                  "key": "123",
                  "mailboxRef": Object {
                    "key": "mailbox-key",
                    "uid": undefined,
                  },
                  "name": "name",
                  "parent": "0",
                  "path": "parent/folder/name",
                  "remoteRef": Object {
                    "internalId": null,
                    "uid": null,
                  },
                  "unread": 0,
                  "writable": true,
                }
            `);
        });

        test("root folder inside a mailshare mailbox", () => {
            const mailbox = {
                writable: true,
                root: "mailshare",
                remoteRef: {},
                key: "mailbox-key",
                type: MailboxType.MAILSHARE
            };
            expect(create("123", "name", null, mailbox)).toMatchInlineSnapshot(`
                Object {
                  "allowSubfolder": true,
                  "default": true,
                  "expanded": false,
                  "imapName": "name",
                  "key": "123",
                  "mailboxRef": Object {
                    "key": "mailbox-key",
                    "uid": undefined,
                  },
                  "name": "name",
                  "parent": null,
                  "path": "mailshare",
                  "remoteRef": Object {
                    "internalId": null,
                    "uid": null,
                  },
                  "unread": 0,
                  "writable": true,
                }
            `);
        });
        test("sub folder inside a mailshare mailbox", () => {
            const mailbox = {
                writable: true,
                root: "mailshare",
                remoteRef: {},
                key: "mailbox-key",
                type: MailboxType.MAILSHARE
            };
            expect(create("123", "name", { key: "0", path: "mailshare/folder", uid: "0" }, mailbox))
                .toMatchInlineSnapshot(`
                Object {
                  "allowSubfolder": true,
                  "default": false,
                  "expanded": false,
                  "imapName": "name",
                  "key": "123",
                  "mailboxRef": Object {
                    "key": "mailbox-key",
                    "uid": undefined,
                  },
                  "name": "name",
                  "parent": "0",
                  "path": "mailshare/folder/name",
                  "remoteRef": Object {
                    "internalId": null,
                    "uid": null,
                  },
                  "unread": 0,
                  "writable": true,
                }
            `);
        });
    });
    describe("isDefault", () => {
        const user = { type: MailboxType.USER, remoteRef: {} };
        const mailshare = { type: MailboxType.MAILSHARE, remoteRef: {} };
        test("INBOX in user mailbox is a default folder", () => {
            expect(create(undefined, "INBOX", undefined, user).default).toBeTruthy();
        });
        test("Sent in user mailbox is a default folder", () => {
            expect(create(undefined, "Sent", undefined, user).default).toBeTruthy();
        });
        test("Drafts in user mailbox is a default folder", () => {
            expect(create(undefined, "Drafts", undefined, user).default).toBeTruthy();
        });
        test("Trash in user mailbox is a default folder", () => {
            expect(create(undefined, "Trash", undefined, user).default).toBeTruthy();
        });
        test("Junk in user mailbox is a default folder", () => {
            expect(create(undefined, "Junk", undefined, user).default).toBeTruthy();
        });
        test("Outbox in user mailbox is a default folder", () => {
            expect(create(undefined, "Outbox", undefined, user).default).toBeTruthy();
        });
        test("Any other root folder in user mailbox is not a folder", () => {
            expect(create(undefined, "Any", undefined, user).default).not.toBeTruthy();
            expect(create(undefined, "inboxe", undefined, user).default).not.toBeTruthy();
        });
        test("All root folder are default folder in mailshare mailbox", () => {
            expect(create(undefined, "Any", undefined, mailshare).default).toBeTruthy();
            expect(create(undefined, "INBOX", undefined, mailshare).default).toBeTruthy();
        });
        test("A sub folder cannot be a default folder ", () => {
            expect(create(undefined, "INBOX", {}, user).default).not.toBeTruthy();
            expect(create(undefined, "Root", {}, mailshare).default).not.toBeTruthy();
        });
    });
    describe("rename", () => {
        test("Rename a root folder", () => {
            expect(rename({ name: "name", path: "name" }, "newName")).toStrictEqual({
                name: "newName",
                path: "newName"
            });
        });
        test("Rename a sub folder", () => {
            expect(rename({ name: "name", path: "parent/name" }, "newName")).toStrictEqual({
                name: "newName",
                path: "parent/newName"
            });
        });
    });
});
