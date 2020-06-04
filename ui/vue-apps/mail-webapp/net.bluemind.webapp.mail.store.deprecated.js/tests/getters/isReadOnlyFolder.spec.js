import { isReadOnlyFolder } from "../../src/getters/isReadOnlyFolder";
import { ItemUri } from "@bluemind/item-uri";

const mailboxUid = "my-mailbox";
const mailshare1Uid = "mailshare1Uid";
const mailshare2Uid = "mailshare2Uid";

const folder1Uid = "folder1Uid";
const folder2Uid = "folder2Uid";
const folder3Uid = "folder3Uid";

const context = {
    state: {
        folders: {
            items: {
                [ItemUri.encode(folder1Uid, mailboxUid)]: { uid: folder1Uid },
                [ItemUri.encode(folder2Uid, mailshare1Uid)]: { uid: folder2Uid },
                [ItemUri.encode(folder3Uid, mailshare2Uid)]: { uid: folder3Uid }
            }
        }
    },
    getters: {
        mailshares: [
            { uid: mailshare1Uid, writable: true },
            { uid: mailshare2Uid, writable: false }
        ],
        my: { mailboxUid },
        ["folders/folders"]: [
            { key: ItemUri.encode(folder1Uid, mailboxUid), uid: folder1Uid },
            { key: ItemUri.encode(folder2Uid, mailshare1Uid), uid: folder2Uid },
            { key: ItemUri.encode(folder3Uid, mailshare2Uid), uid: folder3Uid }
        ]
    }
};

describe("[Mail-WebappStore][getters] : isReadOnlyFolder", () => {
    test("No (classic folder)", () => {
        expect(isReadOnlyFolder(context.state, context.getters)(folder1Uid)).toBe(false);
    });
    test("No (writable mailshare)", () => {
        expect(isReadOnlyFolder(context.state, context.getters)(folder2Uid)).toBe(false);
    });
    test("Yes (read-only mailshare)", () => {
        expect(isReadOnlyFolder(context.state, context.getters)(folder3Uid)).toBe(true);
    });
});
