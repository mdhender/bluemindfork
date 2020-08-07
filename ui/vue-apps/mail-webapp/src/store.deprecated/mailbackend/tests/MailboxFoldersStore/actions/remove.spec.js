import { remove } from "../../../MailboxFoldersStore/actions/remove";

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
        await remove(context, "folderId");
        expect(context.dispatch).toHaveBeenCalledWith(
            "mail/REMOVE_FOLDER",
            { key: "Folder", mailbox: {} },
            { root: true }
        );
    });
    test("With error", async () => {
        context.dispatch.mockRejectedValue("ERROR");
        await expect(remove(context, "folderId")).rejects.toBe("ERROR");
    });
});
