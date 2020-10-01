import actions from "./actions";
import { MessageStatus } from "../../model/message";
import mutations from "./mutations";

export default {
    actions,
    mutations,
    getters: {
        isLoaded(state) {
            return key => state[key].status === MessageStatus.LOADED;
        }
    },
    state: {}
};
