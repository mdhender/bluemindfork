import { TOGGLE_EDIT_FOLDER } from "~mutations";

export default {
    state: {
        editing: undefined
    },
    mutations: {
        [TOGGLE_EDIT_FOLDER]: (state, key) => {
            if (state.editing && state.editing === key) {
                state.editing = undefined;
            } else {
                state.editing = key;
            }
        }
    }
};
