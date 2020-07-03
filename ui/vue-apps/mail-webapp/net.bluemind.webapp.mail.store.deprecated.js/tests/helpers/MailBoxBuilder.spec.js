import { MailBoxBuilder } from "../../src/getters/helpers/MailBoxBuilder";
import { Verb } from "@bluemind/core.container.api";

const getters = {
    "folders/getFoldersByMailbox": jest.fn()
};
const user = {
    type: "mailboxacl",
    ownerDirEntryPath: "bm.lan/users/941ED8F6",
    owner: "941ED8F6",
    uid: "941ED8F6",
    verbs: [Verb.All],
    ownerDisplayName: "Alice",
    name: "Alice"
};
const mailshare = {
    type: "mailboxacl",
    ownerDirEntryPath: "bm.lan/mailshares/D5030EE3",
    name: "Mailshare mailshare",
    owner: "D5030EE3",
    verbs: [Verb.Read],
    ownerDisplayName: "mailshare"
};
const calendar = {
    type: "calendar"
};
describe("[Mail-WebappStore][getters] : mailshares ", () => {
    beforeEach(() => {
        getters["folders/getFoldersByMailbox"].mockClear();
    });
    test("can test if a mailbox is a mailshare", () => {
        expect(MailBoxBuilder.isMailshare(mailshare)).toBeTruthy();
        expect(MailBoxBuilder.isMailshare(user)).not.toBeTruthy();
        expect(MailBoxBuilder.isMailshare(calendar)).not.toBeTruthy();
    });
    test("can test if a mailbox is a mailshare", () => {
        expect(MailBoxBuilder.isUser(mailshare)).not.toBeTruthy();
        expect(MailBoxBuilder.isUser(user)).toBeTruthy();
        expect(MailBoxBuilder.isUser(calendar)).not.toBeTruthy();
    });
    test("can test if it's my mailbox", () => {
        expect(MailBoxBuilder.isMe(mailshare, "mailshare")).not.toBeTruthy();
        expect(MailBoxBuilder.isMe(user, "bob")).not.toBeTruthy();
        expect(MailBoxBuilder.isMe(calendar, "dummy")).not.toBeTruthy();
        expect(MailBoxBuilder.isMe(user, "941ED8F6")).toBeTruthy();
    });
    test("set the mailbox owner UID ", () => {
        expect(MailBoxBuilder.build(user, getters).uid).toEqual(user.owner);
        expect(MailBoxBuilder.build(mailshare, getters).uid).toEqual(mailshare.owner);
    });
    test("set the mailbox UID ", () => {
        expect(MailBoxBuilder.build(user, getters).mailboxUid).toEqual("user." + user.uid);
        expect(MailBoxBuilder.build(mailshare, getters).mailboxUid).toEqual(mailshare.owner);
    });
    test("set mailbox's folders ", () => {
        MailBoxBuilder.build(user, getters);
        expect(getters["folders/getFoldersByMailbox"]).toHaveBeenCalledWith("user." + user.uid);
        MailBoxBuilder.build(mailshare, getters);
        expect(getters["folders/getFoldersByMailbox"]).toHaveBeenCalledWith(mailshare.owner);
    });
    test("set the writable state", () => {
        expect(MailBoxBuilder.build(user, getters).writable).toEqual(true);
        expect(MailBoxBuilder.build(mailshare, getters).writable).toEqual(false);
    });
    test("set mailbox name", () => {
        expect(MailBoxBuilder.build(user, getters).name).toEqual(user.ownerDisplayName);
        expect(MailBoxBuilder.build(mailshare, getters).name).toEqual(mailshare.ownerDisplayName);
    });
    test("set mailbox root", () => {
        expect(MailBoxBuilder.build(user, getters).root).toEqual("");
        expect(MailBoxBuilder.build(mailshare, getters).root).toEqual(mailshare.ownerDisplayName);
    });
    test("set mailbox type", () => {
        expect(MailBoxBuilder.build(user, getters).type).toEqual("user");
        expect(MailBoxBuilder.build(mailshare, getters).type).toEqual("mailshare");
    });
    test("only contains the previously described flags ", () => {
        expect(Object.keys(MailBoxBuilder.build(user, getters)).sort()).toEqual(
            ["writable", "folders", "uid", "mailboxUid", "root", "name", "type"].sort()
        );
    });
});
