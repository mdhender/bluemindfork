import actions from "./actions";
import MessageStatus from "./MessageStatus";
import mutations from "./mutations";

export default {
    actions,
    mutations,
    getters: {
        isLoaded(state) {
            return key => {
                let m = state[key];
                let s = m && state[key].status;
                return s === MessageStatus.LOADED;
            };
        },
        getMessagesByKey(state) {
            return keys => (Array.isArray(keys) ? keys : [keys]).map(key => state[key]);
        }
    },
    state: {}
};
