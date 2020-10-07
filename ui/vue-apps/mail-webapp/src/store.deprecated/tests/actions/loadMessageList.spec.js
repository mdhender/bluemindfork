import ContainerObserver from "@bluemind/containerobserver";
import { loadMessageList } from "../../actions/loadMessageList";
import mutationTypes from "../../../store/mutationTypes";
import actionTypes from "../../../store/actionTypes";

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
            },
            messages: {},
            messageList: {
                messageKeys: [1, 2, 3],
                filter: null,
                search: {
                    pattern: null,
                    folder: null
                }
            }
        },
        session: {
            userSettings: {
                mail_thread: false
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
        expect(context.commit).toHaveBeenCalledWith("mail/" + mutationTypes.SET_SEARCH_PATTERN, undefined, {
            root: true
        });
        expect(context.commit).toHaveBeenCalledWith("currentMessage/clear");
        expect(context.commit).toHaveBeenCalledWith("messages/clearParts");
        expect(context.commit).toHaveBeenCalledWith("mail/" + mutationTypes.CLEAR_MESSAGE_LIST, null, { root: true });
    });
    test("fetch messages on folder select folder", async () => {
        await loadMessageList(context, { folder: folderUid, filter: "all" });
        expect(context.dispatch).toHaveBeenNthCalledWith(1, "selectFolder", folder);
        expect(context.dispatch).toHaveBeenNthCalledWith(
            2,
            "mail/" + actionTypes.FETCH_MESSAGE_LIST_KEYS,
            { conversationsEnabled: false, folder },
            { root: true }
        );
        expect(context.dispatch).toHaveBeenNthCalledWith(
            3,
            "mail/" + actionTypes.FETCH_MESSAGE_METADATA,
            { messageKeys: [1, 2, 3] },
            { root: true }
        );
    });

    test("set default folder to inbox by default", async () => {
        context.rootState.activeFolder = folderUid;
        await loadMessageList(context, {});
        expect(context.dispatch).toHaveBeenNthCalledWith(1, "selectFolder", inbox);
        expect(context.dispatch).toHaveBeenNthCalledWith(
            2,
            "mail/" + actionTypes.FETCH_MESSAGE_LIST_KEYS,
            { conversationsEnabled: false, folder: inbox },
            { root: true }
        );
        expect(context.dispatch).toHaveBeenNthCalledWith(
            3,
            "mail/" + actionTypes.FETCH_MESSAGE_METADATA,
            { messageKeys: [1, 2, 3] },
            { root: true }
        );
    });

    test("fetch only the 40 first mails of the selected folder", async () => {
        context.rootState.mail.messageList.messageKeys = new Array(200).fill(0).map((zero, i) => i);
        await loadMessageList(context, { folder: folderUid });
        expect(context.dispatch).toHaveBeenCalledWith(
            "mail/" + actionTypes.FETCH_MESSAGE_METADATA,
            {
                messageKeys: context.rootState.mail.messageList.messageKeys.slice(0, 40)
            },
            { root: true }
        );
    });

    test("Call search action only if a search pattern is set", async () => {
        await loadMessageList(context, { folder: folderUid, search: '"search pattern"' });
        expect(context.commit).toHaveBeenCalledWith("mail/" + mutationTypes.SET_SEARCH_PATTERN, "search pattern", {
            root: true
        });
    });

    test("Use filter for search or folder fetching if a filter is given", async () => {
        let messageQuery = { folder: folderUid, filter: "unread" };
        await loadMessageList(context, messageQuery);
        expect(context.commit).toHaveBeenCalledWith("mail/" + mutationTypes.SET_MESSAGE_LIST_FILTER, "unread", {
            root: true
        });
    });
});
