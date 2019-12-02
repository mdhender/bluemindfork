import { move } from "../../src/actions/move";

const context = {
    commit: jest.fn(),
    dispatch: jest.fn().mockReturnValue(Promise.resolve({ subject: "dummy" })),
    getters: { my: { mailboxUid: "mailbox-uid" } }
};

describe("[Mail-WebappStore][actions] : move", () => {
    beforeEach(() => {
        context.commit.mockClear();
        context.dispatch.mockClear();
    });
    test("call private move action", done => {
        const messageKey = "message-key",
            folder = { key: "folder-key" };
        move(context, { messageKey, folder }).then(() => {
            expect(context.dispatch).toHaveBeenCalledWith("$_move", { messageKey, destinationKey: "folder-key" });
            done();
        });
        expect(context.dispatch).toHaveBeenCalledWith("$_getIfNotPresent", messageKey);
    });
    test("display alerts", done => {
        const messageKey = "message-key",
            folder = { key: "folder-key" };
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
                    props: { subject: "dummy", folder, folderNameLink: "/mail/" + folder.key + "/" }
                },
                { root: true }
            );
            expect(context.commit).toHaveBeenNthCalledWith(3, "alert/remove", expect.anything(), { root: true });
            done();
        });
    });
    test("create folder if it does not already exists", done => {
        const messageKey = "message-key",
            folder = { value: { name: "folder-name" } };
        move(context, { messageKey, folder }).then(() => {
            expect(context.dispatch).toHaveBeenCalledWith("folders/create", {
                name: "folder-name",
                parentUid: null,
                mailboxUid: "mailbox-uid"
            });
            done();
        });
    });
});
