import { remove } from "../../../src/MailboxFoldersStore/actions/remove";
import ItemUri from "@bluemind/item-uri";
import { REMOVE_FOLDER } from "@bluemind/webapp.mail.store";

const context = {
    rootState: {
        mail: { folders: { folderId: { key: "Folder", mailbox: "user.jdoe" } }, mailboxes: { "user.jdoe": {} } }
    },
    dispatch: jest.fn().mockResolvedValue()
};

describe("[MailFoldersStore][actions] : remove", () => {
    beforeEach(() => {
        context.dispatch.mockClear();
    });
    test("Basic", async () => {
        const folderKey = ItemUri.encode("folderId", "user.jdoe");
        await remove(context, folderKey);
        expect(context.dispatch).toHaveBeenCalledWith(REMOVE_FOLDER, { key: "Folder", mailbox: {} }, { root: true });
    });
    test("With error", async () => {
        context.dispatch.mockRejectedValue("ERROR");
        const folderKey = ItemUri.encode("folderId", "user.jdoe");
        await expect(remove(context, folderKey)).rejects.toBe("ERROR");
    });
});
