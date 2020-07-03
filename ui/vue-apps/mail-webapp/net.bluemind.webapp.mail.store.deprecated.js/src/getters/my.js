import { MailBoxBuilder } from "./helpers/MailBoxBuilder";
import { Verb } from "@bluemind/core.container.api";

export function my(state, getters) {
    let my;
    const mailbox = getters["mailboxes/containers"].find(container => MailBoxBuilder.isMe(container, state.userUid));
    if (mailbox) {
        my = MailBoxBuilder.build(mailbox, getters);
    } else {
        const fake = { ownerDirEntryPath: "/users/", owner: state.userUid, verbs: [Verb.All] };
        my = MailBoxBuilder.build(fake, getters);
    }
    Object.assign(my, getters["folders/getDefaultFolders"](my.mailboxUid));
    return my;
}
