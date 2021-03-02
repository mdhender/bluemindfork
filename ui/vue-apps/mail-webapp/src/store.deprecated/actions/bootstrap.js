import { FETCH_FOLDERS, FETCH_MAILBOXES, FETCH_SIGNATURE } from "~actions";
import { MAILSHARE_KEYS, MY_MAILBOX, MY_MAILBOX_FOLDERS, MAILSHARES } from "~getters";
import WebsocketClient from "@bluemind/sockjs";
import injector from "@bluemind/inject";
import ServerPushHandler from "./ServerPushHandler";

export async function bootstrap({ dispatch, commit, rootGetters, rootState }, userUid) {
    commit("setUserUid", userUid);
    await initUserData(dispatch, rootGetters, rootState);
    const bus = injector.getProvider("GlobalEventBus").get();
    const mailState = rootState.mail;
    const serverPushHandler = await ServerPushHandler.build(bus, mailState, navigator.serviceWorker);
    initWebsocket(serverPushHandler, rootGetters);
}

const initUserData = async (dispatch, rootGetters, rootState) => {
    await dispatch("mail/" + FETCH_MAILBOXES, null, { root: true });

    await dispatch("mail/" + FETCH_FOLDERS, rootGetters["mail/" + MY_MAILBOX], { root: true });
    rootGetters["mail/" + MY_MAILBOX_FOLDERS].forEach(folderKey => dispatch("loadUnreadCount", folderKey));
    await Promise.all(
        rootGetters["mail/" + MAILSHARE_KEYS].map(mailshareKey =>
            dispatch("mail/" + FETCH_FOLDERS, rootState.mail.mailboxes[mailshareKey], { root: true })
        )
    );
    dispatch("loadMailboxConfig");
    dispatch("mail/" + FETCH_SIGNATURE, {}, { root: true });
};

const initWebsocket = (handler, rootGetters) => {
    const socket = new WebsocketClient();
    [rootGetters["mail/" + MY_MAILBOX], ...rootGetters["mail/" + MAILSHARES]].forEach(mailbox => {
        socket.register(`mailreplica.${mailbox.owner}.updated`, handler.handle(mailbox));
    });
};
