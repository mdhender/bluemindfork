import { MailBoxBuilder } from "./helpers/MailBoxBuilder";
import { Verb } from "@bluemind/core.container.api";

export function my(state, getters) {
    let my;
    const name = state.login.split("@")[0];
    const mailbox = getters["mailboxes/containers"].find(container => MailBoxBuilder.isMe(container, name));
    if (mailbox) {
        my = MailBoxBuilder.build(mailbox, getters);
    } else {
        const fake = { ownerDirEntryPath: "/users/", owner: "", verbs: [Verb.All], name };
        my = MailBoxBuilder.build(fake, getters);
    }
    Object.assign(my, getters["folders/getDefaultFolders"](my.mailboxUid));
    return my;
}
