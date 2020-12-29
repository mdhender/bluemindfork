import actions from "./actions";
import { MessageStatus } from "~model/message";
import mutations from "./mutations";
import { MESSAGE_IS_LOADED } from "~getters";

export default {
    actions,
    mutations,
    getters: {
        [MESSAGE_IS_LOADED](state) {
            return key => state[key] && state[key].status === MessageStatus.LOADED;
        }
    },
    state: {}
};
