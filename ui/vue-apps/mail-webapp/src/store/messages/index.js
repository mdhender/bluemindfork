import actions from "./actions";
import mutations from "./mutations";
import { MESSAGE_IS_LOADED, MESSAGE_IS_LOADING } from "~getters";
import { LoadingStatus } from "../../model/loading-status";

export default {
    actions,
    mutations,
    getters: {
        [MESSAGE_IS_LOADED](state) {
            return message => state[key(message)] && state[key(message)].loading === LoadingStatus.LOADED;
        },
        [MESSAGE_IS_LOADING](state) {
            return message => state[key(message)] && state[key(message)].loading === LoadingStatus.LOADING;
        }
    },
    state: {}
};

const key = message => (typeof message === "object" ? message.key : message);
