import { inject } from "@bluemind/inject";
import { convertBlob } from "@bluemind/blob";
import Vue from "vue";

import { FETCH_PART_DATA } from "~/actions";
import { RESET_PARTS_DATA, SET_PART_DATA } from "~/mutations";

export default {
    mutations: {
        [SET_PART_DATA]: (state, { messageKey, address, data }) => {
            if (!state.partsByMessageKey[messageKey]) {
                Vue.set(state.partsByMessageKey, messageKey, {});
            }
            Vue.set(state.partsByMessageKey[messageKey], address, data);
        },
        [RESET_PARTS_DATA]: state => {
            state.partsByMessageKey = {};
            state.quoteNodesByMessageKey = {};
        }
    },

    actions: {
        async [FETCH_PART_DATA]({ commit, state }, { folderUid, imapUid, parts, messageKey }) {
            const service = inject("MailboxItemsPersistence", folderUid);
            const notLoaded = state.partsByMessageKey[messageKey]
                ? parts.filter(
                      part => !Object.prototype.hasOwnProperty.call(state.partsByMessageKey[messageKey], part.address)
                  )
                : parts;

            return Promise.all(
                notLoaded.map(async part => {
                    commit(SET_PART_DATA, { messageKey, data: undefined, address: part.address });

                    const blob = await service.fetch(imapUid, part.address, part.encoding, part.mime, part.charset);
                    const converted = await convertBlob(blob, part);
                    commit(SET_PART_DATA, { messageKey, data: converted, address: part.address });
                })
            );
        }
    },

    state: {
        /**
         * Parts content keyed by message key and by part address.
         * @example partsByMessageKey["messageKey"]["partAddress"] = "partContent"
         */
        partsByMessageKey: {}
    }
};
