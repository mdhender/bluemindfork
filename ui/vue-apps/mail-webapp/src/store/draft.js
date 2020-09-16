import mutationTypes from "./mutationTypes";

export const state = {
    /**
     * Yet we can only have one composer at a time.
     * With thread incoming, if we want to allow user to open multiple composers then we must have here an array of messageCompose
     */
    draft: {
        status: null,
        saveDate: null
    }
};

export const mutations = {
    [mutationTypes.SET_DRAFT_STATUS]: (state, status) => (state.draft.status = status),
    [mutationTypes.SET_DRAFT_SAVE_DATE]: (state, date) => (state.draft.saveDate = date)
};
