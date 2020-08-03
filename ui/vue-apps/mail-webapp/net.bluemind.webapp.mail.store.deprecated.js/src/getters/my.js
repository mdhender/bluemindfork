import { MailBoxBuilder } from "./helpers/MailBoxBuilder";
import { Verb } from "@bluemind/core.container.api";

export function my(state, getters, rootState, rootGetters) {
    let my;
    const mailbox = getters["mailboxes/containers"].find(container => MailBoxBuilder.isMe(container, state.userUid));
    if (mailbox) {
        my = MailBoxBuilder.build(mailbox, rootGetters);
    } else {
        const fake = { ownerDirEntryPath: "/users/", owner: state.userUid, verbs: [Verb.All] };
        my = MailBoxBuilder.build(fake, rootGetters);
    }
    Object.assign(my, rootGetters["mail/MY_DEFAULT_FOLDERS"]);
    return my;
}
