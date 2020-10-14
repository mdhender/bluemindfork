import { MimeType } from "@bluemind/email";
import { inject } from "@bluemind/inject";
import Vue from "vue";

import { FETCH_PART_DATA } from "~/actions";
import { RESET_PARTS_DATA, SET_PART_DATA } from "~/mutations";

export default {
    mutations: {
        [SET_PART_DATA]: (state, { messageKey, address, data }) => {
            if (!state[messageKey]) {
                Vue.set(state, messageKey, {});
            }
            Vue.set(state[messageKey], address, data);
        },
        [RESET_PARTS_DATA]: state => {
            Vue.set(state, {});
        }
    },

    actions: {
        async [FETCH_PART_DATA]({ commit, state }, { folderUid, imapUid, inlines, messageKey }) {
            const service = inject("MailboxItemsPersistence", folderUid);
            const notLoaded = state[messageKey]
                ? inlines.filter(part => !Object.prototype.hasOwnProperty.call(state[messageKey], part.address))
                : inlines;

            return Promise.all(
                notLoaded.map(async part => {
                    const blob = await service.fetch(imapUid, part.address, part.encoding, part.mime, part.charset);
                    const converted =
                        MimeType.isHtml(part) || MimeType.isText(part)
                            ? await convertAsText(blob, part)
                            : await convertToBase64(blob);
                    commit(SET_PART_DATA, { messageKey, data: converted, address: part.address });
                    return Promise.resolve();
                })
            );
        }
    },

    /**
     * Parts content keyed by message key and by part address.
     * @example partsData["messageKey"]["partAddress"] = partData
     */
    state: {}
};

function convertAsText(blob, part) {
    return new Promise(resolve => {
        const reader = new FileReader();
        reader.readAsText(blob, part.charset);
        reader.addEventListener("loadend", e => {
            resolve(e.target.result);
        });
    });
}

function convertToBase64(blob) {
    const reader = new FileReader();
    reader.readAsDataURL(blob);
    return new Promise(resolve => {
        reader.onloadend = () => {
            resolve(reader.result);
        };
    });
}
