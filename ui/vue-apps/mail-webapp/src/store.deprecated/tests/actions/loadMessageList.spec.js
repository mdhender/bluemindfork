import ContainerObserver from "@bluemind/containerobserver";
import { loadMessageList } from "../../actions/loadMessageList";
import { FOLDER_BY_PATH, MY_INBOX } from "~getters";
import { CLEAR_MESSAGE_LIST, SET_ACTIVE_FOLDER, SET_MESSAGE_LIST_FILTER, SET_SEARCH_PATTERN } from "~mutations";
import { FETCH_MESSAGE_LIST_KEYS, FETCH_MESSAGE_METADATA } from "~actions";

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
            messages: { 1: { key: 1 }, 2: { key: 2 }, 3: { key: 3 } },
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
        ["mail/" + FOLDER_BY_PATH]: jest.fn().mockImplementation(path => {
            if (path === "/my/path") {
                return folder;
            } else if (path === "/my/mailshare/path") {
                return folderOfMailshare;
            } else {
                return undefined;
            }
        }),
        ["mail/" + MY_INBOX]: inbox
    }
};

describe("[Mail-WebappStore][actions] :  loadMessageList", () => {
    beforeEach(() => {
        context.dispatch.mockClear();
        context.commit.mockClear();
        context.rootGetters["mail/" + FOLDER_BY_PATH].mockClear();
        context.rootState.mail.activeFolder = inboxUid;
        context.state.messages.itemKeys = [1, 2, 3];
        context.state.messageFilter = null;
        ContainerObserver.observe.mockClear();
        ContainerObserver.forget.mockClear();
    });

    test("locate folder by folderUid", async () => {
        await loadMessageList(context, { folder: folderUid });
        expect(context.rootGetters["mail/" + FOLDER_BY_PATH]).not.toHaveBeenCalled();
        expect(context.commit).toHaveBeenCalledWith("mail/" + SET_ACTIVE_FOLDER, folder.key, expect.anything());
        expect(context.dispatch).toHaveBeenCalledWith("loadUnreadCount", folder.key);

        await loadMessageList(context, { mailshare: folderUidOfMailshare });
        expect(context.rootGetters["mail/" + FOLDER_BY_PATH]).not.toHaveBeenCalled();
        expect(context.commit).toHaveBeenCalledWith(
            "mail/" + SET_ACTIVE_FOLDER,
            folderUidOfMailshare,
            expect.anything()
        );
        expect(context.dispatch).toHaveBeenCalledWith("loadUnreadCount", folderUidOfMailshare);
    });

    test("if locate by key fail, locate folder by path", async () => {
        await loadMessageList(context, { folder: "/my/path" });
        expect(context.rootGetters["mail/" + FOLDER_BY_PATH]).toHaveBeenCalled();
        expect(context.rootGetters["mail/" + FOLDER_BY_PATH]).toHaveBeenCalledTimes(1);
        expect(context.commit).toHaveBeenCalledWith("mail/" + SET_ACTIVE_FOLDER, folder.key, expect.anything());
        expect(context.dispatch).toHaveBeenCalledWith("loadUnreadCount", folder.key);

        context.rootGetters["mail/" + FOLDER_BY_PATH].mockClear();
        await loadMessageList(context, { mailshare: "/my/mailshare/path" });
        expect(context.rootGetters["mail/" + FOLDER_BY_PATH]).toHaveBeenCalled();
        expect(context.rootGetters["mail/" + FOLDER_BY_PATH]).toHaveBeenCalledTimes(1);
        expect(context.commit).toHaveBeenCalledWith(
            "mail/" + SET_ACTIVE_FOLDER,
            folderUidOfMailshare,
            expect.anything()
        );
        expect(context.dispatch).toHaveBeenCalledWith("loadUnreadCount", folderUidOfMailshare);
    });

    test("clear the current context", async () => {
        await loadMessageList(context, {});
        expect(context.commit).toHaveBeenCalledWith("mail/" + SET_SEARCH_PATTERN, undefined, {
            root: true
        });
        expect(context.commit).toHaveBeenCalledWith("currentMessage/clear");
        expect(context.commit).toHaveBeenCalledWith("mail/" + CLEAR_MESSAGE_LIST, null, { root: true });
    });
    test("fetch messages on folder select folder", async () => {
        await loadMessageList(context, { folder: folderUid, filter: "all" });
        expect(context.commit).toHaveBeenCalledWith("mail/" + SET_ACTIVE_FOLDER, folder.key, expect.anything());
        expect(context.dispatch).toHaveBeenNthCalledWith(1, "loadUnreadCount", folder.key);
        expect(context.dispatch).toHaveBeenNthCalledWith(
            2,
            "mail/" + FETCH_MESSAGE_LIST_KEYS,
            { conversationsEnabled: false, folder },
            { root: true }
        );
        expect(context.dispatch).toHaveBeenNthCalledWith(
            3,
            "mail/" + FETCH_MESSAGE_METADATA,
            [{ key: 1 }, { key: 2 }, { key: 3 }],
            { root: true }
        );
    });

    test("set default folder to inbox by default", async () => {
        context.rootState.activeFolder = folderUid;
        await loadMessageList(context, {});
        expect(context.commit).toHaveBeenCalledWith("mail/" + SET_ACTIVE_FOLDER, inbox.key, expect.anything());
        expect(context.dispatch).toHaveBeenNthCalledWith(1, "loadUnreadCount", inbox.key);
        expect(context.dispatch).toHaveBeenNthCalledWith(
            2,
            "mail/" + FETCH_MESSAGE_LIST_KEYS,
            { conversationsEnabled: false, folder: inbox },
            { root: true }
        );
        expect(context.dispatch).toHaveBeenNthCalledWith(
            3,
            "mail/" + FETCH_MESSAGE_METADATA,
            [{ key: 1 }, { key: 2 }, { key: 3 }],
            { root: true }
        );
    });

    test("fetch only the 40 first mails of the selected folder", async () => {
        context.rootState.mail.messageList.messageKeys = new Array(200).fill(0).map((zero, i) => i);
        context.rootState.mail.messageList.messageKeys.forEach(key => {
            context.rootState.mail.messages[key] = { key };
        });
        await loadMessageList(context, { folder: folderUid });
        expect(context.dispatch).toHaveBeenCalledWith(
            "mail/" + FETCH_MESSAGE_METADATA,
            context.rootState.mail.messageList.messageKeys
                .slice(0, 40)
                .map(key => context.rootState.mail.messages[key]),
            { root: true }
        );
    });

    test("Call search action only if a search pattern is set", async () => {
        await loadMessageList(context, { folder: folderUid, search: '"search pattern"' });
        expect(context.commit).toHaveBeenCalledWith("mail/" + SET_SEARCH_PATTERN, "search pattern", {
            root: true
        });
    });

    test("Use filter for search or folder fetching if a filter is given", async () => {
        let messageQuery = { folder: folderUid, filter: "unread" };
        await loadMessageList(context, messageQuery);
        expect(context.commit).toHaveBeenCalledWith("mail/" + SET_MESSAGE_LIST_FILTER, "unread", {
            root: true
        });
    });
});
