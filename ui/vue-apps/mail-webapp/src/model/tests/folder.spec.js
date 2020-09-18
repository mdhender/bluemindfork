const { create } = require("../folder");
const { MailboxType } = require("../mailbox");

describe("Folder model functions", () => {
    describe("create", () => {
        test("root folder inside a user mailbox", () => {
            const mailbox = { writable: true, root: "", remoteRef: {}, key: "mailbox-key", type: MailboxType.USER };
            expect(create("123", "name", null, mailbox)).toMatchInlineSnapshot(`
                Object {
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
        test("root folder inside a user mailbox", () => {
            const mailbox = {
                writable: true,
                root: "mailshare",
                remoteRef: {},
                key: "mailbox-key",
                type: MailboxType.MAILSHARE
            };
            expect(create("123", "name", null, mailbox)).toMatchInlineSnapshot(`
                Object {
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
});
