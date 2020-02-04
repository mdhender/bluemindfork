import * as getters from "./getters";
import * as mutations from "./mutations";

export default {
    namespaced: true,
    state() {
        return {
            id: undefined,
            key: undefined,
            parts: { attachments: [], inlines: [] },
            saveDate: null,
            status: null,
            attachmentStatuses: {},
            attachmentProgresses: {}
        };
    },
    mutations,
    getters
};
