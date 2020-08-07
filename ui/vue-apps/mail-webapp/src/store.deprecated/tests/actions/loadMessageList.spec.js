import ContainerObserver from "@bluemind/containerobserver";
import { loadMessageList } from "../../actions/loadMessageList";

jest.mock("@bluemind/containerobserver");

const mailboxUid = "mailbox:uid",
    folderUid = "folder:uid",
    folder = {
        key: folderUid,
        mailbox: mailboxUid
    };
const inboxUid = "inbox-uid",
    inbox = {
        key: inboxUid,
        mailbox: mailboxUid
    };

const mailshareUid = "mailshare:uid",
    folderUidOfMailshare = "folderOfMailshare:uid",
    folderOfMailshare = {
        key: folderUidOfMailshare,
        mailbox: mailshareUid
    };

const context = {
    dispatch: jest.fn().mockReturnValue(Promise.resolve()),
    commit: jest.fn(),
    state: {
        messages: { itemKeys: [1, 2, 3] },
        sorted: "up to down",
        messageFilter: null,
        draft: { parts: { attachments: [] } },
        search: {
            pattern: null
        }
    },
    rootState: {
        mail: {
            activeFolder: inboxUid,
            folders: {
                [inboxUid]: inbox,
                [folderUid]: folder,
                [folderUidOfMailshare]: folderOfMailshare
            }
        }
    },
    rootGetters: {
        "mail/FOLDER_BY_PATH": jest.fn().mockImplementation(path => {
            if (path === "/my/path") {
                return folder;
            } else if (path === "/my/mailshare/path") {
                return folderOfMailshare;
            } else {
                return undefined;
            }
        }),
        "mail/MY_INBOX": inbox
    }
};

describe("[Mail-WebappStore][actions] :  loadMessageList", () => {
    beforeEach(() => {
        context.dispatch.mockClear();
        context.commit.mockClear();
        context.rootGetters["mail/FOLDER_BY_PATH"].mockClear();
        context.rootState.mail.activeFolder = inboxUid;
        context.state.messages.itemKeys = [1, 2, 3];
        context.state.messageFilter = null;
        ContainerObserver.observe.mockClear();
        ContainerObserver.forget.mockClear();
    });

    test("locate folder by folderUid", async () => {
        await loadMessageList(context, { folder: folderUid });
        expect(context.rootGetters["mail/FOLDER_BY_PATH"]).not.toHaveBeenCalled();
        expect(context.dispatch).toHaveBeenCalledWith("selectFolder", folder);

        await loadMessageList(context, { mailshare: folderUidOfMailshare });
        expect(context.rootGetters["mail/FOLDER_BY_PATH"]).not.toHaveBeenCalled();
        expect(context.dispatch).toHaveBeenCalledWith("selectFolder", folderOfMailshare);
    });

    test("if locate by key fail, locate folder by path", async () => {
        await loadMessageList(context, { folder: "/my/path" });
        expect(context.rootGetters["mail/FOLDER_BY_PATH"]).toHaveBeenCalled();
        expect(context.rootGetters["mail/FOLDER_BY_PATH"]).toHaveBeenCalledTimes(1);
        expect(context.dispatch).toHaveBeenCalledWith("selectFolder", folder);

        context.rootGetters["mail/FOLDER_BY_PATH"].mockClear();
        await loadMessageList(context, { mailshare: "/my/mailshare/path" });
        expect(context.rootGetters["mail/FOLDER_BY_PATH"]).toHaveBeenCalled();
        expect(context.rootGetters["mail/FOLDER_BY_PATH"]).toHaveBeenCalledTimes(1);
        expect(context.dispatch).toHaveBeenCalledWith("selectFolder", folderOfMailshare);
    });

    test("clear the current context", async () => {
        await loadMessageList(context, {});
        expect(context.commit).toHaveBeenCalledWith("search/setStatus", "idle");
        expect(context.commit).toHaveBeenCalledWith("search/setPattern", undefined);
        expect(context.commit).toHaveBeenCalledWith("currentMessage/clear");
        expect(context.commit).toHaveBeenCalledWith("messages/clearParts");
        expect(context.commit).toHaveBeenCalledWith("messages/clearItems");
    });
    test("fetch messages on folder select folder", async () => {
        await loadMessageList(context, { folder: folderUid, filter: "all" });
        expect(context.dispatch).toHaveBeenNthCalledWith(1, "selectFolder", folder);
        expect(context.dispatch).toHaveBeenNthCalledWith(2, "messages/list", {
            sorted: context.state.sorted,
            folderUid,
            filter: "all"
        });
        expect(context.dispatch).toHaveBeenNthCalledWith(3, "messages/multipleByKey", context.state.messages.itemKeys);
    });

    test("set default folder to inbox by default", async () => {
        context.rootState.activeFolder = folderUid;
        await loadMessageList(context, {});
        expect(context.dispatch).toHaveBeenNthCalledWith(1, "selectFolder", inbox);
        expect(context.dispatch).toHaveBeenNthCalledWith(2, "messages/list", {
            sorted: context.state.sorted,
            folderUid: inboxUid
        });
        expect(context.dispatch).toHaveBeenNthCalledWith(3, "messages/multipleByKey", context.state.messages.itemKeys);
    });

    test("fetch only the 40 first mails of the selected folder", async () => {
        context.state.messages.itemKeys = new Array(200).fill(0).map((zero, i) => i);
        await loadMessageList(context, { folder: folderUid });
        expect(context.dispatch).toHaveBeenCalledWith(
            "messages/multipleByKey",
            context.state.messages.itemKeys.slice(0, 40)
        );
    });

    test("Call search action only if a search pattern is set", async () => {
        await loadMessageList(context, { folder: folderUid });
        expect(context.dispatch).not.toHaveBeenCalledWith("search/search", expect.anything());
        context.dispatch.mockClear();
        await loadMessageList(context, { folder: folderUid, search: '"search pattern"' });
        expect(context.dispatch).toHaveBeenCalledWith("search/search", {
            pattern: "search pattern",
            filter: undefined,
            folderKey: undefined
        });
        expect(context.dispatch).not.toHaveBeenCalledWith("message/list", expect.anything());
        expect(context.dispatch).not.toHaveBeenCalledWith("messages/multipleByKey", expect.anything());
    });

    test("Use filter for search or folder fetching if a filter is given", async () => {
        let messageQuery = { folder: folderUid, filter: "unread" };
        await loadMessageList(context, messageQuery);
        expect(context.dispatch).toHaveBeenNthCalledWith(2, "messages/list", {
            sorted: context.state.sorted,
            folderUid: messageQuery.folder,
            filter: messageQuery.filter
        });
        messageQuery = { folder: folderUid, search: '"search pattern"', filter: "unread" };
        await loadMessageList(context, messageQuery);
        expect(context.dispatch).toHaveBeenCalledWith("search/search", {
            pattern: "search pattern",
            filter: messageQuery.filter
        });
    });
});
