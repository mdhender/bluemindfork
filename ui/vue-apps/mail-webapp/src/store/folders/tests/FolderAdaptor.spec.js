import { MailboxType } from "../../helpers/MailboxAdaptor";
import remotefolder from "../../tests/data/remotefolder.json";
import { FolderAdaptor } from "../helpers/FolderAdaptor";

//TODO: rework on folder adaptors
// - test coverage is low
// - interfaces are uncertain and heterogeneous
describe("Folder adaptors", () => {
    test("fromMailboxFolder", () => {
        const mailbox = {
            remoteRef: { uid: "135adc10-db84-440e-aebc-e10d185fa227" },
            root: "inbox",
            type: MailboxType.USER,
            writable: true
        };
        expect(FolderAdaptor.fromMailboxFolder(remotefolder, mailbox)).toMatchInlineSnapshot(`
            Object {
              "default": false,
              "expanded": false,
              "imapName": "Archives",
              "key": "135adc10-db84-440e-aebc-e10d185fa227",
              "mailboxRef": Object {
                "key": undefined,
                "uid": "135adc10-db84-440e-aebc-e10d185fa227",
              },
              "name": "Archives",
              "parent": null,
              "path": "Archives",
              "remoteRef": Object {
                "internalId": 460,
                "uid": "135adc10-db84-440e-aebc-e10d185fa227",
              },
              "unread": 0,
              "writable": true,
            }
        `);
    });

    describe("toMailboxFolder", () => {
        test("Export a local folder in a mailbox withour root remote folder", () => {
            const folder = {
                remoteRef: {
                    internalId: 460,
                    uid: "135adc10-db84-440e-aebc-e10d185fa227"
                },
                name: "Archives",
                path: "Archives"
            };
            const mailbox = {
                root: ""
            };
            expect(FolderAdaptor.toMailboxFolder(folder, mailbox)).toMatchInlineSnapshot(`
                Object {
                  "internalId": 460,
                  "uid": "135adc10-db84-440e-aebc-e10d185fa227",
                  "value": Object {
                    "fullName": "Archives",
                    "name": "Archives",
                    "parentUid": undefined,
                  },
                }
            `);
        });
        test("Export a local folder in a mailbox with a root folder to remote folder", () => {
            const folder = {
                remoteRef: {
                    internalId: 460,
                    uid: "135adc10-db84-440e-aebc-e10d185fa227"
                },
                name: "Archives",
                path: "mailbox/Archives"
            };
            const mailbox = {
                root: "mailbox"
            };
            expect(FolderAdaptor.toMailboxFolder(folder, mailbox)).toMatchInlineSnapshot(`
                Object {
                  "internalId": 460,
                  "uid": "135adc10-db84-440e-aebc-e10d185fa227",
                  "value": Object {
                    "fullName": "Archives",
                    "name": "Archives",
                    "parentUid": undefined,
                  },
                }
            `);
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
});
