import * as actions from "./actions";
import * as getters from "./getters";
import * as mutations from "./mutations";
import * as state from "./state";

export default {
    namespaced: true,
    state: Object.assign({}, state),
    actions,
    mutations,
    getters
};
