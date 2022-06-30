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
            expect(isDefault(true, "INBOX", { type: MailboxType.USER })).toBeTruthy();
        });
        test("Sent in user mailbox is a default folder", () => {
            expect(isDefault(true, "Sent", { type: MailboxType.USER })).toBeTruthy();
        });
        test("Drafts in user mailbox is a default folder", () => {
            expect(isDefault(true, "Drafts", { type: MailboxType.USER })).toBeTruthy();
        });
        test("Trash in user mailbox is a default folder", () => {
            expect(isDefault(true, "Trash", { type: MailboxType.USER })).toBeTruthy();
        });
        test("Templates in user mailbox is a default folder", () => {
            expect(isDefault(true, "Templates", { type: MailboxType.USER })).toBeTruthy();
        });

        test("Junk in user mailbox is a default folder", () => {
            expect(isDefault(true, "Junk", { type: MailboxType.USER })).toBeTruthy();
        });
        test("Outbox in user mailbox is a default folder", () => {
            expect(isDefault(true, "Outbox", { type: MailboxType.USER })).toBeTruthy();
        });
        test("Any other root folder in user mailbox is not a folder", () => {
            expect(isDefault(true, "Any", { type: MailboxType.USER })).not.toBeTruthy();
            expect(isDefault(true, "inboxe", { type: MailboxType.USER })).not.toBeTruthy();
        });
        test("All root folder are default folder in mailshare mailbox", () => {
            expect(isDefault(true, "Any", { type: MailboxType.MAILSHARE })).toBeTruthy();
            expect(isDefault(true, "INBOX", { type: MailboxType.MAILSHARE })).toBeTruthy();
        });
        test("A sub folder cannot be a default folder ", () => {
            expect(isDefault(false, "INBOX", { type: MailboxType.USER })).not.toBeTruthy();
            expect(isDefault(false, "Root", { type: MailboxType.MAILSHARE })).not.toBeTruthy();
        });
    });
});
