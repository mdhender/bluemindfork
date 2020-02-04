import * as getters from "./getters";
import * as mutations from "./mutations";
import { MailboxItemsStore as messages } from "@bluemind/backend.mail.store";

export default {
    namespaced: true,
    state() {
        return {
            id: undefined,
            key: undefined,
            parts: { attachments: [], inlines: [] },
            saveDate: null,
            status: null
        };
    },
    mutations,
    getters
};
