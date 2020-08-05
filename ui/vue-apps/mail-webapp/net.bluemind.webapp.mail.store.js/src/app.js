export const state = {
    activeFolder: undefined
};

export const mutations = {
    SET_ACTIVE_FOLDER: (state, key) => {
        state.activeFolder = key;
    }
};

export const getters = {
    CURRENT_MAILBOX: state => state.mailboxes[state.folders[state.activeFolder].mailbox]
};
