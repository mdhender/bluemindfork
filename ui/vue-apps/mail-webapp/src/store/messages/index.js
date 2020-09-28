import actions from "./actions";
import { MessageStatus } from "../../model/message";
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
