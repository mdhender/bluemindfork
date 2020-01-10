import { MailBoxBuilder } from "../../src/getters/helpers/MailBoxBuilder";

const getters = {
    "folders/getFoldersByMailbox": jest.fn()
};
const user = {
    type: "mailboxacl",
    ownerDirEntryPath: "bm.lan/users/941ED8F6",
    owner: "941ED8F6",
    writable: true,
    name: "alice"
};
const mailshare = {
    type: "mailboxacl",
    ownerDirEntryPath: "bm.lan/mailshares/D5030EE3",
    name: "mailshare",
    owner: "D5030EE3",
    writable: false
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
        expect(MailBoxBuilder.isMe(user, "alice")).toBeTruthy();
    });
    test("set the mailbox owner UID ", () => {
        expect(MailBoxBuilder.build(user, getters).uid).toEqual(user.owner);
        expect(MailBoxBuilder.build(mailshare, getters).uid).toEqual(mailshare.owner);
    });
    test("set the mailbox UID ", () => {
        expect(MailBoxBuilder.build(user, getters).mailboxUid).toEqual("user." + user.name);
        expect(MailBoxBuilder.build(mailshare, getters).mailboxUid).toEqual(mailshare.owner);
    });
    test("set mailbox's folders ", () => {
        MailBoxBuilder.build(user, getters);
        expect(getters["folders/getFoldersByMailbox"]).toHaveBeenCalledWith("user." + user.name);
        MailBoxBuilder.build(mailshare, getters);
        expect(getters["folders/getFoldersByMailbox"]).toHaveBeenCalledWith(mailshare.owner);
    });
    test("set the writable state", () => {
        expect(MailBoxBuilder.build(user, getters).writable).toEqual(user.writable);
        expect(MailBoxBuilder.build(mailshare, getters).writable).toEqual(mailshare.writable);
    });
    test("set mailbox name", () => {
        expect(MailBoxBuilder.build(user, getters).name).toEqual(user.name);
        expect(MailBoxBuilder.build(mailshare, getters).name).toEqual(mailshare.name);
    });
    test("set mailbox root", () => {
        expect(MailBoxBuilder.build(user, getters).root).toEqual("");
        expect(MailBoxBuilder.build(mailshare, getters).root).toEqual(mailshare.name);
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
