import { markFolderAsRead } from "../../actions/markFolderAsRead";

const folderKey = "folder-key";

const context = {
    dispatch: jest.fn().mockResolvedValue(),
    commit: jest.fn(),
    state: {
        messages: {
            items: []
        }
    },
    rootState: {
        mail: {
            folders: {
                [folderKey]: { path: "", key: folderKey }
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
        expect(context.commit).toHaveBeenCalledWith(
            "addApplicationAlert",
            expect.objectContaining({
                code: "MSG_FOLDER_MARKASREAD_LOADING"
            }),
            expect.anything()
        );
        expect(context.dispatch).toHaveBeenCalledWith("folders/markAsRead", folderKey);
        expect(context.commit).toHaveBeenCalledWith("removeApplicationAlert", expect.anything(), expect.anything());
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
        context.dispatch.mockRejectedValueOnce();
        await markFolderAsRead(context, folderKey);
        expect(context.commit).toHaveBeenCalledWith(
            "addApplicationAlert",
            expect.objectContaining({
                code: "MSG_FOLDER_MARKASREAD_LOADING"
            }),
            expect.anything()
        );
        expect(context.dispatch).toHaveBeenCalledWith("folders/markAsRead", folderKey);
        expect(context.commit).toHaveBeenCalledWith("removeApplicationAlert", expect.anything(), expect.anything());
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
