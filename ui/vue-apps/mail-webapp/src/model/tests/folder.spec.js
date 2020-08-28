const { create } = require("../folder");
const { MailboxType } = require("../mailbox");

describe("Folder model functions", () => {
    describe("create", () => {
        test("root folder inside a user mailbox", () => {
            const mailbox = { writable: true, root: "", key: "mailbox-key", type: MailboxType.USER };
            expect(create("123", "name", null, mailbox)).toMatchInlineSnapshot(`
            Object {
              "default": false,
              "expanded": false,
              "id": null,
              "imapName": "name",
              "key": "123",
              "mailbox": "mailbox-key",
              "name": "name",
              "parent": null,
              "path": "name",
              "uid": null,
              "unread": 0,
              "writable": true,
            }
        `);
        });
        test("sub folder inside a user mailbox", () => {
            const mailbox = { writable: true, root: "", key: "mailbox-key", type: MailboxType.USER };
            expect(create("123", "name", { key: "0", path: "parent/folder", uid: "0" }, mailbox))
                .toMatchInlineSnapshot(`
            Object {
              "default": false,
              "expanded": false,
              "id": null,
              "imapName": "name",
              "key": "123",
              "mailbox": "mailbox-key",
              "name": "name",
              "parent": "0",
              "path": "parent/folder/name",
              "uid": null,
              "unread": 0,
              "writable": true,
            }
        `);
        });
        test("root folder inside a user mailbox", () => {
            const mailbox = { writable: true, root: "mailshare", key: "mailbox-key", type: MailboxType.MAILSHARE };
            expect(create("123", "name", null, mailbox)).toMatchInlineSnapshot(`
            Object {
              "default": true,
              "expanded": false,
              "id": null,
              "imapName": "name",
              "key": "123",
              "mailbox": "mailbox-key",
              "name": "name",
              "parent": null,
              "path": "mailshare",
              "uid": null,
              "unread": 0,
              "writable": true,
            }
        `);
        });

        test("root folder inside a mailshare mailbox", () => {
            const mailbox = { writable: true, root: "mailshare", key: "mailbox-key", type: MailboxType.MAILSHARE };
            expect(create("123", "name", null, mailbox)).toMatchInlineSnapshot(`
            Object {
              "default": true,
              "expanded": false,
              "id": null,
              "imapName": "name",
              "key": "123",
              "mailbox": "mailbox-key",
              "name": "name",
              "parent": null,
              "path": "mailshare",
              "uid": null,
              "unread": 0,
              "writable": true,
            }
        `);
        });
        test("sub folder inside a mailshare mailbox", () => {
            const mailbox = { writable: true, root: "mailshare", key: "mailbox-key", type: MailboxType.MAILSHARE };
            expect(create("123", "name", { key: "0", path: "mailshare/folder", uid: "0" }, mailbox))
                .toMatchInlineSnapshot(`
            Object {
              "default": false,
              "expanded": false,
              "id": null,
              "imapName": "name",
              "key": "123",
              "mailbox": "mailbox-key",
              "name": "name",
              "parent": "0",
              "path": "mailshare/folder/name",
              "uid": null,
              "unread": 0,
              "writable": true,
            }
        `);
        });
    });
});
