import { move } from "../../src/actions/move";

const context = {
    commit: jest.fn(),
    dispatch: jest.fn(),
    getters: {
        my: { mailboxUid: "mailbox-uid" },
        "folders/getFolderByKey": jest
            .fn()
            .mockReturnValue({ key: "folder-key", value: { name: "folderName", fullName: "Full/name" } })
    }
};

describe("[Mail-WebappStore][actions] : move", () => {
    beforeEach(() => {
        context.commit.mockClear();
        context.dispatch.mockClear();
        context.dispatch.mockReturnValueOnce(Promise.resolve([{ subject: "dummy" }]));
        context.dispatch.mockReturnValueOnce("folder-key");
        context.getters["folders/getFolderByKey"].mockClear();
    });
    test("call private move action", done => {
        const messageKey = "message-key",
            folder = { key: "folder-key" };
        move(context, { messageKey, folder }).then(() => {
            expect(context.getters["folders/getFolderByKey"]).toHaveBeenCalledWith("folder-key");
            expect(context.dispatch).toHaveBeenCalledWith("$_move", {
                messageKeys: [messageKey],
                destinationKey: "folder-key"
            });
            done();
        });
        expect(context.dispatch).toHaveBeenCalledWith("$_getIfNotPresent", [messageKey]);
    });
    test("display alerts", done => {
        const messageKey = "message-key",
            folder = { key: "folder-key", value: { name: "folderName", fullName: "Full/name" } };
        move(context, { messageKey, folder }).then(() => {
            expect(context.commit).toHaveBeenNthCalledWith(
                1,
                "alert/add",
                {
                    code: "MSG_MOVED_LOADING",
                    props: { subject: "dummy" },
                    uid: expect.anything()
                },
                { root: true }
            );
            expect(context.commit).toHaveBeenNthCalledWith(
                2,
                "alert/add",
                {
                    code: "MSG_MOVE_OK",
                    props: {
                        subject: "dummy",
                        folder: folder.value,
                        folderNameLink: { name: "v:mail:home", params: { folder: folder.value.fullName } }
                    }
                },
                { root: true }
            );
            expect(context.commit).toHaveBeenNthCalledWith(3, "alert/remove", expect.anything(), { root: true });
            done();
        });
    });
    test("call createFolder private action", done => {
        const messageKey = "message-key",
            folder = { value: { name: "folder-name" } };
        move(context, { messageKey, folder }).then(() => {
            expect(context.dispatch).toHaveBeenCalledWith("$_createFolder", folder);
            done();
        });
    });
});
