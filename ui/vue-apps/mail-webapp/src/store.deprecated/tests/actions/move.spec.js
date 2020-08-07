import { move } from "../../actions/move";
import ItemUri from "@bluemind/item-uri";

ItemUri.container = jest.fn().mockReturnValue("");

const mailboxUid = "mboxUid";
const context = {
    commit: jest.fn(),
    dispatch: jest.fn(),
    state: {
        messages: {
            items: {
                "message-key": {}
            }
        }
    },
    rootState: {
        mail: {
            folders: {
                "folder-key": {
                    path: "/my/path",
                    key: "folder-key",
                    name: "folderName"
                }
            }
        }
    },
    rootGetters: {
        "mail/MY_MAILBOX_KEY": mailboxUid,
        "mail/MAILSHARE_KEYS": []
    }
};

describe("[Mail-WebappStore][actions] : move", () => {
    beforeEach(() => {
        context.commit.mockClear();
        context.dispatch.mockClear();
        context.dispatch.mockReturnValueOnce(Promise.resolve([{ subject: "dummy" }]));
        context.dispatch.mockReturnValueOnce("folder-key");
    });

    test("call private move action", async () => {
        const messageKey = "message-key",
            folder = { key: "folder-key" };
        await move(context, { messageKey, folder });
        expect(context.dispatch).toHaveBeenCalledWith("$_getIfNotPresent", [messageKey]);
        expect(context.dispatch).toHaveBeenCalledWith("$_move", {
            messageKeys: [messageKey],
            destinationKey: "folder-key"
        });
    });

    test("display alerts", done => {
        const messageKey = "message-key",
            folder = { key: "folder-key", name: "folderName", path: "/my/path" };
        move(context, { messageKey, folder }).then(() => {
            expect(context.commit).toHaveBeenNthCalledWith(
                1,
                "addApplicationAlert",
                {
                    code: "MSG_MOVED_LOADING",
                    props: { subject: "dummy" },
                    uid: expect.anything()
                },
                { root: true }
            );
            expect(context.commit).toHaveBeenNthCalledWith(
                2,
                "addApplicationAlert",
                {
                    code: "MSG_MOVE_OK",
                    props: {
                        subject: "dummy",
                        folder: folder,
                        folderNameLink: { name: "v:mail:home", params: { folder: folder.path } }
                    }
                },
                { root: true }
            );
            expect(context.commit).toHaveBeenNthCalledWith(3, "removeApplicationAlert", expect.anything(), {
                root: true
            });
            done();
        });
    });

    test("call createFolder private action", done => {
        const messageKey = "message-key",
            folder = { value: { name: "folder-name" } };
        move(context, { messageKey, folder }).then(() => {
            expect(context.dispatch).toHaveBeenCalledWith("$_createFolder", {
                folder,
                mailboxUid: mailboxUid
            });
            done();
        });
    });
});
