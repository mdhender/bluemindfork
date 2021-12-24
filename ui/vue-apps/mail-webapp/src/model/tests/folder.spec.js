const { create, rename, compare, match } = require("../folder");
const { MailboxType } = require("../mailbox");
import injector from "@bluemind/inject";

injector.register({ provide: "i18n", use: { t: n => n } });
describe("Folder model functions", () => {
    describe("create", () => {
        test("root folder inside a user mailbox", () => {
            const mailbox = { writable: true, root: "", remoteRef: {}, key: "mailbox-key", type: MailboxType.USER };
            expect(create("123", "name", null, mailbox)).toMatchInlineSnapshot(`
                Object {
                  "allowConversations": true,
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
                  "allowConversations": true,
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
                  "allowConversations": false,
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
                  "allowConversations": false,
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
        const mailshare = { type: MailboxType.MAILSHARE, remoteRef: {}, root: "mailshareRoot" };
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
        test("Templates in user mailbox is a default folder", () => {
            expect(create(undefined, "Templates", undefined, user).default).toBeTruthy();
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
    describe("compare", () => {
        const mailboxRef = { key: "mbk1" };
        const mailboxRef2 = { key: "mbk2" };
        const sortedFolders = [
            { imapName: "INBOX", path: "INBOX", mailboxRef },
            { imapName: "a", path: "a", mailboxRef },
            { imapName: "a", path: "a/a", parent: "a", mailboxRef },
            { imapName: "a", path: "a/a/a", parent: "a/a", mailboxRef },
            { imapName: "a", path: "a/a/INBOX", parent: "a/a", mailboxRef },
            { imapName: "b", path: "a/b", parent: "a", mailboxRef },
            { imapName: "a", path: "a/b/a", parent: "a/b", mailboxRef },
            { imapName: "toto", path: "toto", mailboxRef },
            { imapName: "inOtherMailbox", path: "a/inOtherMailbox", parent: "a", mailboxRef: mailboxRef2 }
        ];
        test("sort", () => {
            const unsortedFolders = [
                { imapName: "a", path: "a/b/a", parent: "a/b", mailboxRef },
                { imapName: "INBOX", path: "INBOX", mailboxRef },
                { imapName: "a", path: "a/a/a", parent: "a/a", mailboxRef },
                { imapName: "toto", path: "toto", mailboxRef },
                { imapName: "b", path: "a/b", parent: "a", mailboxRef },
                { imapName: "inOtherMailbox", path: "a/inOtherMailbox", parent: "a", mailboxRef: mailboxRef2 },
                { imapName: "a", path: "a/a", parent: "a", mailboxRef },
                { imapName: "a", path: "a", mailboxRef },
                { imapName: "a", path: "a/a/INBOX", parent: "a/a", mailboxRef }
            ];
            const result = unsortedFolders.sort(compare);
            expect(result).toEqual(sortedFolders);
        });
    });
    describe.only("match", () => {
        test.skip("match test folder name with wildcard", () => {
            let folder = { name: "MyTest", imapName: "Yean", path: "/path/" };
            expect(match(folder, "my")).toBeTruthy();
            expect(match(folder, "tesT")).toBeTruthy();
            expect(match(folder, "YTes")).toBeTruthy();
            expect(match(folder, "mytest")).toBeTruthy();
            expect(match(folder, "Pouic")).toBeFalsy();
        });
        test.skip("match test folder imapName with wildcard", () => {
            let folder = { name: "Yeah", imapName: "MyTest", path: "/path/" };
            expect(match(folder, "my")).toBeTruthy();
            expect(match(folder, "TeSt")).toBeTruthy();
            expect(match(folder, "yTeS")).toBeTruthy();
            expect(match(folder, "MYTEst")).toBeTruthy();
            expect(match(folder, "Pouic")).toBeFalsy();
        });
        test("match search pattern at the end of the path", () => {
            let folder = { name: "MyTest", imapName: "Yeah", path: "/start/path/MyTest" };
            expect(match(folder, "ath/My")).toBeTruthy();
            expect(match(folder, "path/My")).toBeTruthy();
            expect(match(folder, "start/My")).toBeFalsy();
            expect(match(folder, "a/path/My")).toBeFalsy();
            expect(match(folder, "tart/path/MyT")).toBeTruthy();
            expect(match(folder, "path/Test")).toBeFalsy();
            expect(match(folder, "path/MyTest/Other")).toBeFalsy();
        });
        test("match to search for imap name at the end of the path", () => {
            let folder = { name: "MyTest", imapName: "Yeah", path: "/start/path/MyTest" };
            expect(match(folder, "ath/My")).toBeTruthy();
            expect(match(folder, "path/My")).toBeTruthy();
            expect(match(folder, "start/My")).toBeFalsy();
            expect(match(folder, "a/path/My")).toBeFalsy();
            expect(match(folder, "tart/path/MyT")).toBeTruthy();
            expect(match(folder, "path/Test")).toBeFalsy();
            expect(match(folder, "path/MyTest/Other")).toBeFalsy();
        });
    });
});
