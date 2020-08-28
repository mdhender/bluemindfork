import { markFolderAsRead } from "../../actions/markFolderAsRead";
import inject from "@bluemind/inject";
import { MockMailboxFoldersClient } from "@bluemind/test-utils";

const foldersService = new MockMailboxFoldersClient();
inject.register({ provide: "MailboxFoldersPersistence", factory: () => foldersService });

const folderKey = "folder-key";
const context = {
    dispatch: jest.fn().mockResolvedValue(),
    commit: jest.fn(),
    getters: {
        "messages/messages": []
    },
    rootState: {
        mail: {
            folders: {
                [folderKey]: { path: "", key: folderKey, id: "id" }
            }
        }
    }
};

describe("[Mail-WebappStore][actions] : markFolderAsRead", () => {
    beforeEach(() => {
        context.dispatch.mockClear();
        context.commit.mockClear();
    });

    test("Basic", async () => {
        await markFolderAsRead(context, folderKey);
        expect(foldersService.markFolderAsRead).toHaveBeenCalledWith("id");
        expect(context.commit).toHaveBeenCalledWith(
            "addApplicationAlert",
            expect.objectContaining({
                code: "MSG_FOLDER_MARKASREAD_SUCCESS"
            }),
            expect.anything()
        );
        expect(context.commit).not.toHaveBeenCalledWith(
            "addApplicationAlert",
            expect.objectContaining({
                code: "MSG_FOLDER_MARKASREAD_ERROR"
            }),
            expect.anything()
        );
    });

    test("With Error", async () => {
        foldersService.markFolderAsRead.mockRejectedValueOnce();
        await markFolderAsRead(context, folderKey);
        expect(foldersService.markFolderAsRead).toHaveBeenCalledWith("id");
        expect(context.commit).not.toHaveBeenCalledWith(
            "addApplicationAlert",
            expect.objectContaining({
                code: "MSG_FOLDER_MARKASREAD_SUCCESS"
            }),
            expect.anything()
        );
        expect(context.commit).toHaveBeenCalledWith(
            "addApplicationAlert",
            expect.objectContaining({
                code: "MSG_FOLDER_MARKASREAD_ERROR"
            }),
            expect.anything()
        );
    });
});
