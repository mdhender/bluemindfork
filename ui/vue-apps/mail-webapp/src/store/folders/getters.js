import {
    FOLDERS,
    FOLDER_BY_PATH,
    FOLDERS_BY_PATH,
    FOLDER_GET_DESCENDANTS,
    FOLDER_HAS_CHILDREN,
    IS_DESCENDANT,
    FOLDER_GET_CHILDREN
} from "~/getters";
import { folder } from "@bluemind/mail";
import { Cache } from "~/utils/cache";

const { compare } = folder;

export default {
    [FOLDERS]: state => Object.values(state).sort(compare),
    [FOLDERS_BY_PATH]: (state, getters) => {
        const foldersByPath = getters[FOLDERS].reduce(
            (cache, folder) => cache.get(folder.path.toUpperCase()).push(folder) && cache,
            new Cache(() => [])
        );
        return path => foldersByPath.get(path.toUpperCase());
    },
    [FOLDER_BY_PATH]: (state, getters) => (path, mailbox) =>
        getters[FOLDERS_BY_PATH](path).find(f => f.mailboxRef.key === mailbox.key),
    [IS_DESCENDANT]: state => (parentKey, key) => {
        const parent = state[parentKey];
        const folder = state[key];
        return (
            parent &&
            folder &&
            parent.mailboxRef.key === folder.mailboxRef.key &&
            folder.path.startsWith(parent.path + "/")
        );
    },
    [FOLDER_HAS_CHILDREN]: (state, { FOLDER_GET_CHILDREN }) => folder => FOLDER_GET_CHILDREN(folder).length > 0,
    [FOLDER_GET_CHILDREN]: (state, { FOLDERS }) => {
        const folderByParent = FOLDERS.reduce((byParent, folder) => {
            byParent[folder.parent] ? byParent[folder.parent].push(folder) : (byParent[folder.parent] = [folder]);
            return byParent;
        }, {});
        return ({ key }) => folderByParent[key] || [];
    },
    [FOLDER_GET_DESCENDANTS]: (state, { FOLDER_GET_CHILDREN }) => folder => {
        const descendants = FOLDER_GET_CHILDREN(folder);
        let i = 0;
        while (i < descendants.length) {
            descendants.push(...FOLDER_GET_CHILDREN(descendants[i]));
            i++;
        }
        return descendants;
    }
};
