export function currentMailbox(state, getters, rootState) {
    const activeFolder = rootState.mail.activeFolder;
    return rootState.mail.mailboxes[rootState.mail.folders[activeFolder].mailbox];
}
