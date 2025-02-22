import { loadingStatusUtils } from "@bluemind/mail";
import actions from "./actions";
import mutations from "./mutations";
import { MESSAGE_IS_LOADED, MESSAGE_IS_LOADING } from "~/getters";

const { LoadingStatus } = loadingStatusUtils;

export default {
    actions,
    mutations,
    getters: {
        [MESSAGE_IS_LOADED](state) {
            return message => state[key(message)] && state[key(message)].loading === LoadingStatus.LOADED;
        },
        [MESSAGE_IS_LOADING](state) {
            return message => [LoadingStatus.NOT_LOADED, LoadingStatus.LOADING].includes(state[key(message)]?.loading);
        }
    },
    state: {}
};

const key = message => (typeof message === "object" ? message.key : message);
