import { selectFolder } from "../../actions/selectFolder";

jest.mock("@bluemind/containerobserver");

const context = {
    commit: jest.fn(),
    dispatch: jest.fn().mockReturnValue(Promise.resolve()),
    rootState: {
        mail: {
            activeFolder: folderUid
        }
    }
};

const folderUid = "folder:uid",
    folder = { key: folderUid, mailbox: "mailbox:uid" };

describe("[Mail-WebappStore][actions] :  selectFolder", () => {
    beforeEach(() => {
        context.commit.mockClear();
        context.dispatch.mockClear();
        context.rootState.mail.activeFolder = folderUid;
    });

    test("to set current folder only if folder has changed", async () => {
        await selectFolder(context, folder);
        expect(context.commit).not.toHaveBeenCalledWith("mail/SET_ACTIVE_FOLDER", expect.anything());
        expect(context.dispatch).not.toHaveBeenCalledWith("loadUnreadCount", folderUid);
        const another = { key: "anotherKey", mailbox: "anotherMailbox" };

        await selectFolder(context, another);
        expect(context.commit).toHaveBeenCalledWith("mail/SET_ACTIVE_FOLDER", another.key, { root: true });
        expect(context.dispatch).toHaveBeenCalledWith("loadUnreadCount", another.key);
    });
});
