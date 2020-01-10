import { TreeBuilder } from "./helpers/TreeBuilder";
import flatMap from "lodash.flatmap";

export function tree(state, getters) {
    const tree = {};
    let nodes = getters.my.folders.map(folder => TreeBuilder.toTreeItem(folder, state.foldersData[folder.uid] || {}));
    tree.my = TreeBuilder.build(nodes);
    nodes = flatMap(getters.mailshares, ms => getters["folders/getFoldersByMailbox"](ms.mailboxUid)).map(folder =>
        TreeBuilder.toTreeItem(folder, state.foldersData[folder.uid] || {})
    );
    tree.mailshares = TreeBuilder.build(nodes);
    return tree;
}
