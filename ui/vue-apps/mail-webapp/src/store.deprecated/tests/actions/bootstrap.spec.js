import { FETCH_FOLDERS, FETCH_MAILBOXES } from "~actions";
import { MAILSHARE_KEYS, MY_MAILBOX, MY_MAILBOX_FOLDERS, MAILSHARES } from "~getters";
import { bootstrap } from "../../actions/bootstrap";
import { FETCH_SIGNATURE } from "../../../store/types/actions";
import WebsocketClient from "@bluemind/sockjs";
import ServerPushHandler from "../../actions/ServerPushHandler";
import injector from "@bluemind/inject";

jest.mock("@bluemind/sockjs");
jest.mock("@bluemind/inject");
jest.mock("../../actions/ServerPushHandler");

const myMailbox = { key: "mailbox:uid", owner: "mailbox:uid" },
    mailshareKeys = ["A", "B"],
    myMailboxFolderKeys = ["1", "2", "3"],
    mailshares = [{ owner: "A" }, { owner: "B" }];
const context = {
    dispatch: jest.fn().mockReturnValue(Promise.resolve()),
    commit: jest.fn(),
    rootGetters: {
        ["mail/" + MY_MAILBOX]: myMailbox,
        ["mail/" + MY_MAILBOX_FOLDERS]: myMailboxFolderKeys,
        ["mail/" + MAILSHARE_KEYS]: mailshareKeys,
        ["mail/" + MAILSHARES]: mailshares
    },
    rootState: { mail: { mailboxes: { [myMailbox.key]: myMailbox, A: { key: "A" }, B: { key: "B" } } } }
};
const bus = jest.fn();
injector.getProvider.mockReturnValue({
    get: () => bus
});

describe("[Mail-WebappStore][actions] :  bootstrap", () => {
    beforeEach(() => {
        context.dispatch.mockClear();
        context.commit.mockClear();
        WebsocketClient.mockClear();
        ServerPushHandler.mockClear();
    });

    test("load all folders from my mailbox and get unread count", done => {
        bootstrap(context).then(() => {
            expect(context.dispatch).toHaveBeenNthCalledWith(1, "mail/" + FETCH_MAILBOXES, null, {
                root: true
            });
            expect(context.dispatch).toHaveBeenNthCalledWith(2, "mail/" + FETCH_FOLDERS, myMailbox, { root: true });
            myMailboxFolderKeys.forEach(key => expect(context.dispatch).toHaveBeenCalledWith("loadUnreadCount", key));
            done();
        });
    });

    test("load all folders from mailshares", done => {
        bootstrap(context).then(() => {
            expect(context.dispatch).toHaveBeenNthCalledWith(1, "mail/" + FETCH_MAILBOXES, null, {
                root: true
            });
            mailshareKeys.forEach(key =>
                expect(context.dispatch).toHaveBeenCalledWith(
                    "mail/" + FETCH_FOLDERS,
                    context.rootState.mail.mailboxes[key],
                    { root: true }
                )
            );
            done();
        });
    });

    test("setup server push handler", done => {
        bootstrap(context).then(() => {
            expect(ServerPushHandler.mock.instances[0]).toBeTruthy();
            expect(ServerPushHandler.mock.calls.length).toBe(1);
            expect(ServerPushHandler.mock.calls[0].length).toBe(3);
            expect(ServerPushHandler.mock.calls[0][0]).toBe(bus);
            expect(ServerPushHandler.mock.calls[0][1]).toBe(context.rootState.mail);
            done();
        });
    });

    test("register to mailbox events", done => {
        bootstrap(context).then(() => {
            const websocketInstance = WebsocketClient.mock.instances[0].register;
            const handlerInstance = ServerPushHandler.mock.instances[0].handle;
            expect(websocketInstance).toHaveBeenNthCalledWith(1, `mailreplica.${myMailbox.owner}.updated`, undefined);
            expect(handlerInstance).toHaveBeenNthCalledWith(1, myMailbox);
            expect(websocketInstance).toHaveBeenNthCalledWith(2, `mailreplica.${mailshares[0].owner}.updated`, undefined);
            expect(handlerInstance).toHaveBeenNthCalledWith(2, mailshares[0]);
            expect(websocketInstance).toHaveBeenNthCalledWith(3, `mailreplica.${mailshares[1].owner}.updated`, undefined);
            expect(handlerInstance).toHaveBeenNthCalledWith(3, mailshares[1]);
            done();
        });
    });

    test("set user uid", () => {
        bootstrap(context, "userUid");
        expect(context.commit).toHaveBeenCalledWith("setUserUid", "userUid");
    });

    test("fetch signature", async () => {
        await bootstrap(context);
        expect(context.dispatch).toHaveBeenNthCalledWith(9, "mail/" + FETCH_SIGNATURE, {}, { root: true });
    });
});
