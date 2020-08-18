import { bootstrap } from "../../actions/bootstrap";

const myMailbox = { key: "mailbox:uid" },
    mailshareKeys = ["A", "B"],
    myMailboxFolderKeys = ["1", "2", "3"];
const context = {
    dispatch: jest.fn().mockReturnValue(Promise.resolve()),
    commit: jest.fn(),
    rootGetters: {
        "mail/MY_MAILBOX": myMailbox,
        "mail/MY_MAILBOX_FOLDERS": myMailboxFolderKeys,
        "mail/MAILSHARE_KEYS": mailshareKeys
    },
    rootState: { mail: { mailboxes: { [myMailbox.key]: myMailbox, A: { key: "A" }, B: { key: "B" } } } }
};

describe("[Mail-WebappStore][actions] :  bootstrap", () => {
    beforeEach(() => {
        context.dispatch.mockClear();
        context.commit.mockClear();
    });

    test("load all folders from my mailbox and get unread count", done => {
        bootstrap(context).then(() => {
            expect(context.dispatch).toHaveBeenNthCalledWith(1, "mail/FETCH_MAILBOXES", null, { root: true });
            expect(context.dispatch).toHaveBeenNthCalledWith(2, "mail/FETCH_FOLDERS", myMailbox, { root: true });
            myMailboxFolderKeys.forEach(key => expect(context.dispatch).toHaveBeenCalledWith("loadUnreadCount", key));
            done();
        });
    });

    test("load all folders from mailshares", done => {
        bootstrap(context).then(() => {
            expect(context.dispatch).toHaveBeenNthCalledWith(1, "mail/FETCH_MAILBOXES", null, { root: true });
            mailshareKeys.forEach(key =>
                expect(context.dispatch).toHaveBeenCalledWith(
                    "mail/FETCH_FOLDERS",
                    context.rootState.mail.mailboxes[key],
                    { root: true }
                )
            );
            done();
        });
    });

    test("set user uid", () => {
        bootstrap(context, "userUid");
        expect(context.commit).toHaveBeenCalledWith("setUserUid", "userUid");
    });
});
