import { FOLDER_BY_PATH, FOLDER_HAS_CHILDREN } from "~getters";

export default {
    [FOLDER_BY_PATH]: state => path => Object.values(state).find(folder => folder.path === path),
    [FOLDER_HAS_CHILDREN]: state => key => !!Object.values(state).find(folder => folder.parent === key)
};
