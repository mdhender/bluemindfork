import { FOLDERS_BY_UPPERCASE_PATH, FOLDER_HAS_CHILDREN, IS_DESCENDANT, FOLDER_GET_CHILDREN } from "~/getters";

export default {
    [FOLDERS_BY_UPPERCASE_PATH]: state => {
        const foldersByPath = {};
        Object.values(state).forEach(folder => (foldersByPath[folder.path.toUpperCase()] = folder));
        return foldersByPath;
    },
    [IS_DESCENDANT]: state => (parentKey, key) => {
        const parent = state[parentKey];
        const folder = state[key];
        return (
            parent && folder && parent.mailboxRef.key === folder.mailboxRef.key && folder.path.startsWith(parent.path)
        );
    },
    [FOLDER_HAS_CHILDREN]: (state, { FOLDER_GET_CHILDREN }) => folder => FOLDER_GET_CHILDREN(folder).length > 0,
    [FOLDER_GET_CHILDREN]: state => {
        const folderByParent = Object.values(state).reduce((byParent, folder) => {
            byParent[folder.parent] ? byParent[folder.parent].push(folder) : (byParent[folder.parent] = [folder]);
            return byParent;
        }, {});
        return ({ key }) => folderByParent[key] || [];
    }
};
