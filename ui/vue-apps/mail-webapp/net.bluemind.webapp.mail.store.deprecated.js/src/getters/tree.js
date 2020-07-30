import { TreeBuilder } from "./helpers/TreeBuilder";

export function tree(state, getters, rootState) {
    const tree = {};
    let nodes = getters.my.folders.map(folder =>
        TreeBuilder.toTreeItem(
            folder,
            true,
            rootState.mail.folders[folder.uid].expanded,
            rootState.mail.folders[folder.uid].unread
        )
    );
    tree.my = TreeBuilder.build(nodes);
    nodes = [];
    getters.mailshares.forEach(mailshare => {
        getters["folders/getFoldersByMailbox"](mailshare.mailboxUid).map(folder =>
            nodes.push(
                TreeBuilder.toTreeItem(
                    folder,
                    mailshare.writable,
                    rootState.mail.folders[folder.uid].expanded,
                    rootState.mail.folders[folder.uid].unread
                )
            )
        );
    });
    tree.mailshares = TreeBuilder.build(nodes);
    return tree;
}
