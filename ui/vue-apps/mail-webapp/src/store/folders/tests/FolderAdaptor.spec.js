import { folderUtils, mailboxUtils } from "@bluemind/mail";

import remotefolder from "../../tests/data/remotefolder.json";
import { FolderAdaptor } from "../helpers/FolderAdaptor";

const { MailboxType } = mailboxUtils;
const { isDefault } = folderUtils;

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
              "allowConversations": true,
              "allowSubfolder": true,
              "default": false,
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
              "unread": undefined,
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
            expect(isDefault(null, "INBOX", "INBOX", { type: MailboxType.USER })).toBeTruthy();
        });
        test("Sent in user mailbox is a default folder", () => {
            expect(isDefault(null, "Sent", "Sent", { type: MailboxType.USER })).toBeTruthy();
        });
        test("Drafts in user mailbox is a default folder", () => {
            expect(isDefault(null, "Drafts", "Drafts", { type: MailboxType.USER })).toBeTruthy();
        });
        test("Trash in user mailbox is a default folder", () => {
            expect(isDefault(null, "Trash", "Trash", { type: MailboxType.USER })).toBeTruthy();
        });
        test("Templates in user mailbox is a default folder", () => {
            expect(isDefault(null, "Templates", "Templates", { type: MailboxType.USER })).toBeTruthy();
        });

        test("Junk in user mailbox is a default folder", () => {
            expect(isDefault(null, "Junk", "Junk", { type: MailboxType.USER })).toBeTruthy();
        });
        test("Outbox in user mailbox is a default folder", () => {
            expect(isDefault(null, "Outbox", "Outbox", { type: MailboxType.USER })).toBeTruthy();
        });
        test("Any other root folder in user mailbox is not a folder", () => {
            expect(isDefault(null, "Any", "Any", { type: MailboxType.USER })).toBeFalsy();
            expect(isDefault(null, "inboxe", "inboxe", { type: MailboxType.USER })).toBeFalsy();
        });
        test("Detect default folders in mailshares", () => {
            expect(isDefault(null, "shared/other/INBOX", "INBOX", { type: MailboxType.MAILSHARE })).toBeFalsy();
            expect(isDefault("parentUid", "shared/SENT", "Sent", { type: MailboxType.MAILSHARE })).toBeTruthy();
        });
        test("Detect default folders in groups", () => {
            expect(isDefault(null, "group/other/INBOX", "INBOX", { type: MailboxType.GROUP })).toBeFalsy();
            expect(isDefault("parentUid", "group/SENT", "Sent", { type: MailboxType.GROUP })).toBeTruthy();
        });
        test("A sub folder cannot be a default folder ", () => {
            expect(isDefault("parentUid", "INBOX", "INBOX", { type: MailboxType.USER })).toBeFalsy();
        });
    });
});
