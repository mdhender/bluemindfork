import { FOLDERS_BY_UPPERCASE_PATH, FOLDER_HAS_CHILDREN, IS_DESCENDANT } from "~getters";

export default {
    [FOLDERS_BY_UPPERCASE_PATH]: state => {
        const foldersByPath = {};
        Object.values(state).forEach(folder => (foldersByPath[folder.path.toUpperCase()] = folder));
        return foldersByPath;
    },
    [FOLDER_HAS_CHILDREN]: state => key => !!Object.values(state).find(folder => folder.parent === key),
    [IS_DESCENDANT]: state => (parentKey, key) => {
        const parent = state[parentKey];
        const folder = state[key];
        return (
            parent && folder && parent.mailboxRef.key === folder.mailboxRef.key && folder.path.startsWith(parent.path)
        );
    }
};
