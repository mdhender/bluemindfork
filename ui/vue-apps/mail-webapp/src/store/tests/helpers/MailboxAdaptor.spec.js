import { MailboxAdaptor } from "../../helpers/MailboxAdaptor";
import containers from "../data/users/alice/containers.json";

jest.mock("postal-mime", () => ({ TextEncoder: jest.fn() }));

describe("MailboxAdaptor", () => {
    describe("fromMailboxContainer", () => {
        const mailshare = containers.find(({ type, name }) => type === "mailboxacl" && name === "read.write");
        const mailshareDirEntry = { displayName: "my mailshare", email: "mail@share.net" };
        const user = containers.find(({ type, name }) => type === "mailboxacl" && name === "alice");
        const userDirEntry = { displayName: "Alice ", email: "alice@mails.net" };
        test("mailshare mailbox", () => {
            expect(MailboxAdaptor.fromMailboxContainer(mailshare, mailshareDirEntry)).toMatchInlineSnapshot(`
                Object {
                  "address": "mail@share.net",
                  "dn": "my mailshare",
                  "key": "2814CC5D-D372-4F66-A434-89863E99B8CD",
                  "loading": "NOT-LOADED",
                  "name": "my mailshare",
                  "offlineSync": false,
                  "owner": "2814CC5D-D372-4F66-A434-89863E99B8CD",
                  "remoteRef": Object {
                    "id": 48,
                    "uid": "2814CC5D-D372-4F66-A434-89863E99B8CD",
                  },
                  "root": "my mailshare",
                  "type": "mailshares",
                  "writable": true,
                }
            `);
        });
        test("user mailbox", () => {
            const mailbox = MailboxAdaptor.fromMailboxContainer(user, userDirEntry);
            expect(mailbox).toMatchInlineSnapshot(`
                Object {
                  "address": "alice@mails.net",
                  "dn": "Alice ",
                  "key": "user.6793466E-F5D4-490F-97BF-DF09D3327BF4",
                  "loading": "NOT-LOADED",
                  "name": "alice@mails.net",
                  "offlineSync": false,
                  "owner": "6793466E-F5D4-490F-97BF-DF09D3327BF4",
                  "remoteRef": Object {
                    "id": 41,
                    "uid": "user.6793466E-F5D4-490F-97BF-DF09D3327BF4",
                  },
                  "root": "",
                  "type": "users",
                  "writable": true,
                }
            `);
        });
    });
});
