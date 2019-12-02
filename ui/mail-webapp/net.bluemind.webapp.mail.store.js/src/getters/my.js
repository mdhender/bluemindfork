export function my(state, getters) {
    const my = {};
    my.mailboxUid = "user." + state.login.split("@")[0];
    Object.assign(my, getters["folders/getDefaultFolders"](my.mailboxUid));
    my.folders = getters["folders/getFoldersByMailbox"](my.mailboxUid);
    return my;
}
