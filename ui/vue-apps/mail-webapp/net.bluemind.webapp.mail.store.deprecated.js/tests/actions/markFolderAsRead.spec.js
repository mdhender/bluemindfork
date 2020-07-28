import { markFolderAsRead } from "../../src/actions/markFolderAsRead";
import ItemUri from "@bluemind/item-uri";

const key = ItemUri.encode("folderUid", "mailbox");

const context = {
    dispatch: jest.fn().mockResolvedValue(),
    commit: jest.fn(),
    getters: {
        "folders/getFolderByKey": jest.fn().mockReturnValue({ value: { fullName: "INBOX" } }),
        unreadCount: jest.fn()
    },
    state: {
        messages: {
            items: []
        }
    }
};

describe("[Mail-WebappStore][actions] : markFolderAsRead", () => {
    beforeEach(() => {
        context.dispatch.mockClear();
        context.commit.mockClear();
        context.getters["folders/getFolderByKey"].mockClear();
    });
    test("Basic", async () => {
        await markFolderAsRead(context, key);
        expect(context.commit).toHaveBeenCalledWith(
            "addApplicationAlert",
            expect.objectContaining({
                code: "MSG_FOLDER_MARKASREAD_LOADING"
            }),
            expect.anything()
        );
        expect(context.dispatch).toHaveBeenCalledWith("folders/markAsRead", key);
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
        await markFolderAsRead(context, key);
        expect(context.commit).toHaveBeenCalledWith(
            "addApplicationAlert",
            expect.objectContaining({
                code: "MSG_FOLDER_MARKASREAD_LOADING"
            }),
            expect.anything()
        );
        expect(context.dispatch).toHaveBeenCalledWith("folders/markAsRead", key);
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
