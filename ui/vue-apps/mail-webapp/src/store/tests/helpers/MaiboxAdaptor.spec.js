import { MailboxAdaptor, MailboxType } from "../../helpers/MailboxAdaptor";
import containers from "../data/users/alice/containers.json";
import { Verb } from "@bluemind/core.container.api";

describe("MailboxAdaptor", () => {
    describe("fromMailboxContainer", () => {
        const mailshare = containers.find(({ type, name }) => type === "mailboxacl" && name === "read.write");
        const user = containers.find(({ type, name }) => type === "mailboxacl" && name === "alice");
        test("mailshare mailbox", () => {
            expect(MailboxAdaptor.fromMailboxContainer(mailshare)).toMatchInlineSnapshot(`
                Object {
                  "key": "2814CC5D-D372-4F66-A434-89863E99B8CD",
                  "name": "read.write",
                  "owner": "2814CC5D-D372-4F66-A434-89863E99B8CD",
                  "remoteRef": Object {
                    "uid": "2814CC5D-D372-4F66-A434-89863E99B8CD",
                  },
                  "root": "read.write",
                  "type": "mailshares",
                  "writable": true,
                }
            `);
        });
        test("user mailbox", () => {
            const mailbox = MailboxAdaptor.fromMailboxContainer(user);
            expect(mailbox).toMatchInlineSnapshot(`
                Object {
                  "key": "user.6793466E-F5D4-490F-97BF-DF09D3327BF4",
                  "name": "Alice",
                  "owner": "6793466E-F5D4-490F-97BF-DF09D3327BF4",
                  "remoteRef": Object {
                    "uid": "user.6793466E-F5D4-490F-97BF-DF09D3327BF4",
                  },
                  "root": "",
                  "type": "users",
                  "writable": true,
                }
            `);
        });
    });
    describe("toMailboxContainer", () => {
        test("mailshare mailbox", () => {
            expect(
                MailboxAdaptor.toMailboxContainer({
                    type: MailboxType.MAILSHARE,
                    owner: "boss",
                    name: "mailbox",
                    writable: false
                })
            ).toStrictEqual({
                ownerDirEntryPath: "/mailshares",
                owner: "boss",
                ownerDisplayname: "mailbox",
                verbs: [Verb.Read],
                type: "mailboxacl"
            });
        });
        test("user mailbox", () => {
            expect(
                MailboxAdaptor.toMailboxContainer({
                    type: MailboxType.USER,
                    owner: "boss",
                    name: "mailbox",
                    writable: true
                })
            ).toStrictEqual({
                ownerDirEntryPath: "/users",
                owner: "boss",
                ownerDisplayname: "mailbox",
                verbs: [Verb.Write],
                type: "mailboxacl"
            });
        });
    });
});
