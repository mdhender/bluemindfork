import FolderHelper from "../src/FolderHelper";
import ItemUri from "@bluemind/item-uri";

const myMailboxUid = "my mailbox";
const folder1 = {
    key: ItemUri.encode(myMailboxUid, "folder1"),
    value: { fullName: "folder1-blue" },
    parentUid: myMailboxUid
};
const folder2 = {
    key: ItemUri.encode(myMailboxUid, "folder2"),
    value: { fullName: "folder2-orange" },
    parentUid: myMailboxUid
};
const folder3 = {
    key: ItemUri.encode(myMailboxUid, "folder3"),
    value: { fullName: "folder3-blue" },
    parentUid: myMailboxUid
};
const myFolders = [folder1, folder2, folder3];
const myMailbox = { mailboxUid: myMailboxUid, folders: myFolders };

const mailshareMailbox = "shared box 1";
const mailshareRoot = "Groot";
const mailshareFolder1 = {
    key: ItemUri.encode(mailshareMailbox, "mailshareFolder1"),
    value: { fullName: "m-folder1-blue" },
    parentUid: mailshareMailbox
};
const mailshareFolder2 = {
    key: ItemUri.encode(mailshareMailbox, "mailshareFolder2"),
    value: { fullName: "m-folder2-orange" },
    parentUid: mailshareMailbox
};
const mailshareFolder3 = {
    key: ItemUri.encode(mailshareMailbox, "mailshareFolder3"),
    value: { fullName: "m-folder3-blue" },
    parentUid: mailshareMailbox
};
const mailshareFolders = [mailshareFolder1, mailshareFolder2, mailshareFolder3];
const sharedMaiboxes = [
    { mailboxUid: mailshareMailbox, type: "mailshare", root: mailshareRoot, folders: mailshareFolders }
];

const mailboxes = [myMailbox].concat(sharedMaiboxes);

describe("[Mail-WebappStore][helper] : FolderHelper.applyFilterThenSliceAndTransform ", () => {
    test("filter folders by name", () => {
        expect(
            FolderHelper.applyFilterThenSliceAndTransform(mailboxes, f => f.value.fullName.includes("blue"), 10)
        ).toEqual([
            FolderHelper.toFolderItem(folder1, false, folder1.value.fullName),
            FolderHelper.toFolderItem(folder3, false, folder3.value.fullName),
            FolderHelper.toFolderItem(mailshareFolder1, true, mailshareRoot + "/" + mailshareFolder1.value.fullName),
            FolderHelper.toFolderItem(mailshareFolder3, true, mailshareRoot + "/" + mailshareFolder3.value.fullName)
        ]);
    });
    test("filter folders with a limit", () => {
        expect(
            FolderHelper.applyFilterThenSliceAndTransform(mailboxes, f => f.value.fullName.includes("blue"), 2)
        ).toEqual([
            FolderHelper.toFolderItem(folder1, false, folder1.value.fullName),
            FolderHelper.toFolderItem(folder3, false, folder3.value.fullName)
        ]);
    });
});
