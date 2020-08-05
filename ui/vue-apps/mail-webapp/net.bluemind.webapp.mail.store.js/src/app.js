export const state = {
    activeFolder: undefined
};

export const mutations = {
    SET_CURRENT_FOLDER: (state, key) => {
        state.activeFolder = key;
    }
};
