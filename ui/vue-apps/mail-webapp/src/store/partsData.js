import { MimeType } from "@bluemind/email";
import { inject } from "@bluemind/inject";
import Vue from "vue";

import { FETCH_PART_DATA } from "~/actions";
import { SET_MESSAGE_INLINE_PARTS_BY_CAPABILITIES, RESET_PARTS_DATA, SET_PART_DATA } from "~/mutations";

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
        },
        // Listeners
        [SET_MESSAGE_INLINE_PARTS_BY_CAPABILITIES]: (state, { key, inlinePartsByCapabilities }) => {
            inlinePartsByCapabilities.forEach(({ parts }) => {
                parts.forEach(({ address }) => {
                    if (state.partsByMessageKey[key]) {
                        Vue.delete(state.partsByMessageKey[key], address);
                    }
                });
            });
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
                    const converted =
                        MimeType.isHtml(part) ||
                        MimeType.isText(part) ||
                        MimeType.MESSAGE_DISPOSITION_NOTIFICATION === part.mime ||
                        MimeType.MESSAGE_DELIVERY_STATUS === part.mime
                            ? await convertAsText(blob, part)
                            : await convertToBase64(blob);
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
