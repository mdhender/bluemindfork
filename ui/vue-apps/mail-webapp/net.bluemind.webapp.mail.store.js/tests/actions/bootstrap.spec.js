import { bootstrap } from "../../src/actions/bootstrap";

const context = {
    dispatch: jest.fn().mockReturnValue(Promise.resolve()),
    commit: jest.fn(),
    state: {
        currentFolderKey: "key"
    },
    getters: {
        my: {
            INBOX: {
                uid: "inbox_uid",
                key: "inbox_key"
            },
            mailboxUid: "mailbox:uid",
            folders: [{ uid: "1" }, { uid: "2" }, { uid: "3" }, { uid: "4" }, { uid: "5" }, { uid: "6" }]
        },
        mailshares: []
    }
};

describe("[Mail-WebappStore][actions] :  bootstrap", () => {
    beforeEach(() => {
        context.dispatch.mockClear();
        context.commit.mockClear();
        context.state.currentFolderKey = "key";
    });
    test("load all folders from my mailbox", done => {
        bootstrap(context).then(() => {
            expect(context.dispatch).toHaveBeenNthCalledWith(1, "folders/all", "mailbox:uid");
            done();
        });
    });
    test("load all folders from mailshares", done => {
        context.mailshares = [{ uid: "a" }, { uid: "b" }, { uid: "c" }];
        bootstrap(context).then(() => {
            expect(context.dispatch).toHaveBeenNthCalledWith(1, "folders/all", "mailbox:uid");
            done();
        });
    });
    test("load unread count on all", done => {
        bootstrap(context).then(() => {
            context.getters.my.folders.forEach((folder, index) => {
                expect(context.dispatch).toHaveBeenNthCalledWith(index + 2, "loadUnreadCount", folder.uid);
            });
            done();
        });
    });
    test("set default folder to inbox if not present", done => {
        bootstrap(context)
            .then(() => {
                expect(context.dispatch).not.toHaveBeenCalledWith("loadMessageList", { folder: "inbox_key" });
                context.state.currentFolderKey = undefined;
                return bootstrap(context, {});
            })
            .then(() => {
                expect(context.dispatch).toHaveBeenCalledWith("loadMessageList", { folder: "inbox_key" });
                done();
            });
    });
    test("set login", () => {
        bootstrap(context, "mylogin@bluemind.lan");
        expect(context.commit).toHaveBeenCalledWith("setUserLogin", "mylogin@bluemind.lan");
    });
});
