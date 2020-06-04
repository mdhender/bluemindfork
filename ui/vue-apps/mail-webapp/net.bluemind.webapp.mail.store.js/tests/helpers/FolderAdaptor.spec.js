import { FolderAdaptor } from "../../src/helpers/FolderAdaptor";
import remotefolder from "../data/remotefolder.json";
import { MailboxType } from "../../src/helpers/MailboxAdaptor";

describe("FolderAdaptor", () => {
    test("fromMailboxFolder", () => {
        const mailbox = {
            uid: "135adc10-db84-440e-aebc-e10d185fa227",
            root: "inbox",
            type: MailboxType.USER,
            writable: true
        };
        expect(FolderAdaptor.fromMailboxFolder(remotefolder, mailbox)).toMatchInlineSnapshot(`
            Object {
              "default": false,
              "id": 460,
              "key": "135adc10-db84-440e-aebc-e10d185fa227",
              "mailbox": "135adc10-db84-440e-aebc-e10d185fa227",
              "name": "Archives",
              "parent": null,
              "path": "inbox/Archives",
              "uid": "135adc10-db84-440e-aebc-e10d185fa227",
              "writable": true,
            }
        `);
    });
    describe("toMailboxFolder", () => {
        test("Export a local folder in a mailbox withour root remote folder", () => {
            const folder = {
                id: 460,
                uid: "135adc10-db84-440e-aebc-e10d185fa227",
                name: "Archives",
                path: "Archives"
            };
            const mailbox = {
                root: ""
            };
            expect(FolderAdaptor.toMailboxFolder(folder, null, mailbox)).toMatchInlineSnapshot(`
                            Object {
                              "internalId": 460,
                              "uid": "135adc10-db84-440e-aebc-e10d185fa227",
                              "value": Object {
                                "fullName": "Archives",
                                "name": "Archives",
                                "parentUid": null,
                              },
                            }
                    `);
        });
        test("Export a local folder in a mailbox with a root folder to remote folder", () => {
            const folder = {
                id: 460,
                uid: "135adc10-db84-440e-aebc-e10d185fa227",
                name: "Archives",
                path: "mailbox/Archives"
            };
            const mailbox = {
                root: "mailbox"
            };
            expect(FolderAdaptor.toMailboxFolder(folder, null, mailbox)).toMatchInlineSnapshot(`
                            Object {
                              "internalId": 460,
                              "uid": "135adc10-db84-440e-aebc-e10d185fa227",
                              "value": Object {
                                "fullName": "Archives",
                                "name": "Archives",
                                "parentUid": null,
                              },
                            }
                    `);
        });
        test("Temporarily hack to export local folder key as remote folder uid", () => {
            const folder = {
                id: 460,
                key: "135adc10-db84-440e-aebc-e10d185fa227",
                name: "Archives",
                path: "mailboxrootArchives"
            };
            const mailbox = {
                root: "inbox"
            };
            expect(FolderAdaptor.toMailboxFolder(folder, null, mailbox).uid).toEqual(folder.key);
        });
    });
    describe("isDefault", () => {
        test("INBOX in user mailbox is a default folder", () => {
            expect(FolderAdaptor.isDefault(true, "INBOX", { type: MailboxType.USER })).toBeTruthy();
        });
        test("Sent in user mailbox is a default folder", () => {
            expect(FolderAdaptor.isDefault(true, "Sent", { type: MailboxType.USER })).toBeTruthy();
        });
        test("Drafts in user mailbox is a default folder", () => {
            expect(FolderAdaptor.isDefault(true, "Drafts", { type: MailboxType.USER })).toBeTruthy();
        });
        test("Trash in user mailbox is a default folder", () => {
            expect(FolderAdaptor.isDefault(true, "Trash", { type: MailboxType.USER })).toBeTruthy();
        });
        test("Junk in user mailbox is a default folder", () => {
            expect(FolderAdaptor.isDefault(true, "Junk", { type: MailboxType.USER })).toBeTruthy();
        });
        test("Outbox in user mailbox is a default folder", () => {
            expect(FolderAdaptor.isDefault(true, "Outbox", { type: MailboxType.USER })).toBeTruthy();
        });
        test("Any other root folder in user mailbox is not a folder", () => {
            expect(FolderAdaptor.isDefault(true, "Any", { type: MailboxType.USER })).not.toBeTruthy();
            expect(FolderAdaptor.isDefault(true, "inboxe", { type: MailboxType.USER })).not.toBeTruthy();
        });
        test("All root folder are default folder in mailshare mailbox", () => {
            expect(FolderAdaptor.isDefault(true, "Any", { type: MailboxType.MAILSHARE })).toBeTruthy();
            expect(FolderAdaptor.isDefault(true, "INBOX", { type: MailboxType.MAILSHARE })).toBeTruthy();
        });
        test("A sub folder cannot be a default folder ", () => {
            expect(FolderAdaptor.isDefault(false, "INBOX", { type: MailboxType.USER })).not.toBeTruthy();
            expect(FolderAdaptor.isDefault(false, "Root", { type: MailboxType.MAILSHARE })).not.toBeTruthy();
        });
    });
    describe("rename", () => {
        test("Rename a root folder", () => {
            expect(FolderAdaptor.rename({ name: "name", path: "name" }, "newName")).toStrictEqual({
                name: "newName",
                path: "newName"
            });
        });
        test("Rename a sub folder", () => {
            expect(FolderAdaptor.rename({ name: "name", path: "parent/name" }, "newName")).toStrictEqual({
                name: "newName",
                path: "parent/newName"
            });
        });
    });
    describe("create", () => {
        test("root folder inside a user mailbox", () => {
            const mailbox = { writable: true, root: "", key: "mailbox-key", type: MailboxType.USER };
            expect(FolderAdaptor.create("123", "name", null, mailbox)).toMatchInlineSnapshot(`
                Object {
                  "default": false,
                  "id": null,
                  "key": "123",
                  "mailbox": "mailbox-key",
                  "name": "name",
                  "parent": null,
                  "path": "name",
                  "uid": null,
                  "writable": true,
                }
            `);
        });
        test("sub folder inside a user mailbox", () => {
            const mailbox = { writable: true, root: "", key: "mailbox-key", type: MailboxType.USER };
            expect(FolderAdaptor.create("123", "name", { key: "0", path: "parent/folder", uid: "0" }, mailbox))
                .toMatchInlineSnapshot(`
                Object {
                  "default": false,
                  "id": null,
                  "key": "123",
                  "mailbox": "mailbox-key",
                  "name": "name",
                  "parent": "0",
                  "path": "parent/folder/name",
                  "uid": null,
                  "writable": true,
                }
            `);
        });
        test("root folder inside a user mailbox", () => {
            const mailbox = { writable: true, root: "mailshare", key: "mailbox-key", type: MailboxType.MAILSHARE };
            expect(FolderAdaptor.create("123", "name", null, mailbox)).toMatchInlineSnapshot(`
                Object {
                  "default": true,
                  "id": null,
                  "key": "123",
                  "mailbox": "mailbox-key",
                  "name": "name",
                  "parent": null,
                  "path": "mailshare/name",
                  "uid": null,
                  "writable": true,
                }
            `);
        });

        test("root folder inside a mailshare mailbox", () => {
            const mailbox = { writable: true, root: "mailshare", key: "mailbox-key", type: MailboxType.MAILSHARE };
            expect(FolderAdaptor.create("123", "name", null, mailbox)).toMatchInlineSnapshot(`
                Object {
                  "default": true,
                  "id": null,
                  "key": "123",
                  "mailbox": "mailbox-key",
                  "name": "name",
                  "parent": null,
                  "path": "mailshare/name",
                  "uid": null,
                  "writable": true,
                }
            `);
        });
        test("sub folder inside a mailshare mailbox", () => {
            const mailbox = { writable: true, root: "mailshare", key: "mailbox-key", type: MailboxType.MAILSHARE };
            expect(FolderAdaptor.create("123", "name", { key: "0", path: "mailshare/folder", uid: "0" }, mailbox))
                .toMatchInlineSnapshot(`
                Object {
                  "default": false,
                  "id": null,
                  "key": "123",
                  "mailbox": "mailbox-key",
                  "name": "name",
                  "parent": "0",
                  "path": "mailshare/folder/name",
                  "uid": null,
                  "writable": true,
                }
            `);
        });
    });
});
