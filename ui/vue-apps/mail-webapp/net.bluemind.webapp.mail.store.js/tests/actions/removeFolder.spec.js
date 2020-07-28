import { removeFolder } from "../../src/actions/removeFolder";

const context = {
    dispatch: jest.fn().mockResolvedValue(),
    commit: jest.fn(),
    getters: {
        "folders/getFolderByKey": jest.fn().mockReturnValue({ value: { fullName: "INBOX" } })
    }
};

describe("[Mail-WebappStore][actions] : removeFolder", () => {
    beforeEach(() => {
        context.dispatch.mockClear();
        context.commit.mockClear();
        context.getters["folders/getFolderByKey"].mockClear();
    });
    test("Basic", async () => {
        await removeFolder(context, "key");
        expect(context.commit).toHaveBeenCalledWith(
            "addApplicationAlert",
            expect.objectContaining({
                code: "MSG_FOLDER_REMOVE_LOADING"
            }),
            expect.anything()
        );
        expect(context.dispatch).toHaveBeenCalledWith("folders/remove", "key");
        expect(context.commit).toHaveBeenCalledWith("removeApplicationAlert", expect.anything(), expect.anything());
        expect(context.commit).toHaveBeenCalledWith(
            "addApplicationAlert",
            expect.objectContaining({
                code: "MSG_FOLDER_REMOVE_SUCCESS"
            }),
            expect.anything()
        );
        expect(context.commit).not.toHaveBeenCalledWith(
            "addApplicationAlert",
            expect.objectContaining({
                code: "MSG_FOLDER_REMOVE_ERROR"
            }),
            expect.anything()
        );
    });
    test("With Error", async () => {
        context.dispatch.mockRejectedValueOnce();
        await removeFolder(context, "key");
        expect(context.commit).toHaveBeenCalledWith(
            "addApplicationAlert",
            expect.objectContaining({
                code: "MSG_FOLDER_REMOVE_LOADING"
            }),
            expect.anything()
        );
        expect(context.dispatch).toHaveBeenCalledWith("folders/remove", "key");
        expect(context.commit).toHaveBeenCalledWith("removeApplicationAlert", expect.anything(), expect.anything());
        expect(context.commit).not.toHaveBeenCalledWith(
            "addApplicationAlert",
            expect.objectContaining({
                code: "MSG_FOLDER_REMOVE_SUCCESS"
            }),
            expect.anything()
        );
        expect(context.commit).toHaveBeenCalledWith(
            "addApplicationAlert",
            expect.objectContaining({
                code: "MSG_FOLDER_REMOVE_ERROR"
            }),
            expect.anything()
        );
    });
});
