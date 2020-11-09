import { REMOVE_FOLDER } from "~actions";
import { removeFolder } from "../../actions/removeFolder";

const folderKey = "folder:key";
const mailbox = { key: "mailbox:key" };
const context = {
    dispatch: jest.fn().mockResolvedValue(),
    commit: jest.fn(),
    rootState: {
        mail: {
            folders: { [folderKey]: { key: folderKey, mailboxRef: { key: mailbox.key } } },
            mailboxes: { [mailbox.key]: mailbox }
        }
    }
};

describe("[Mail-WebappStore][actions] : removeFolder", () => {
    beforeEach(() => {
        context.dispatch.mockClear();
        context.commit.mockClear();
    });

    test("Basic", async () => {
        await removeFolder(context, folderKey);
        expect(context.dispatch).toHaveBeenCalledWith(
            "mail/" + REMOVE_FOLDER,
            { key: folderKey, mailbox },
            { root: true }
        );
        expect(context.commit).toHaveBeenCalledWith(
            "alert/addApplicationAlert",
            expect.objectContaining({
                code: "MSG_FOLDER_REMOVE_SUCCESS"
            }),
            expect.anything()
        );
        expect(context.commit).not.toHaveBeenCalledWith(
            "alert/addApplicationAlert",
            expect.objectContaining({
                code: "MSG_FOLDER_REMOVE_ERROR"
            }),
            expect.anything()
        );
    });

    test("With Error", async () => {
        context.dispatch.mockRejectedValueOnce();
        await removeFolder(context, folderKey);
        expect(context.dispatch).toHaveBeenCalledWith(
            "mail/" + REMOVE_FOLDER,
            { key: folderKey, mailbox },
            { root: true }
        );
        expect(context.commit).not.toHaveBeenCalledWith(
            "alert/addApplicationAlert",
            expect.objectContaining({
                code: "MSG_FOLDER_REMOVE_SUCCESS"
            }),
            expect.anything()
        );
        expect(context.commit).toHaveBeenCalledWith(
            "alert/addApplicationAlert",
            expect.objectContaining({
                code: "MSG_FOLDER_REMOVE_ERROR"
            }),
            expect.anything()
        );
    });
});
