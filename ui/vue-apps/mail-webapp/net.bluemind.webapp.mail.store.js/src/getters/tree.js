import { TreeBuilder } from "./helpers/TreeBuilder";

export function tree(state, getters) {
    const tree = {};
    let nodes = getters.my.folders.map(folder =>
        TreeBuilder.toTreeItem(folder, state.foldersData[folder.uid] || {}, true)
    );
    tree.my = TreeBuilder.build(nodes);
    nodes = [];
    getters.mailshares.forEach(mailshare => {
        getters["folders/getFoldersByMailbox"](mailshare.mailboxUid).map(folder =>
            nodes.push(TreeBuilder.toTreeItem(folder, state.foldersData[folder.uid] || {}, mailshare.writable))
        );
    });
    tree.mailshares = TreeBuilder.build(nodes);
    return tree;
}
