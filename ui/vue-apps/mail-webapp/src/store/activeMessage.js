import { MimeType } from "@bluemind/email";
import { inject } from "@bluemind/inject";

import { FETCH_ACTIVE_MESSAGE_INLINE_PARTS } from "~actions";
import { RESET_ACTIVE_MESSAGE, SET_ACTIVE_MESSAGE_PART_DATA } from "~mutations";

export default {
    mutations: {
        [SET_ACTIVE_MESSAGE_PART_DATA]: (state, { address, data }) => {
            state.partsDataByAddress[address] = data;
        },
        [RESET_ACTIVE_MESSAGE]: state => {
            state.partsDataByAddress = {};
        }
    },

    actions: {
        async [FETCH_ACTIVE_MESSAGE_INLINE_PARTS]({ commit, state }, { folderUid, imapUid, inlines }) {
            const service = inject("MailboxItemsPersistence", folderUid);
            const notLoaded = inlines.filter(
                part => !Object.prototype.hasOwnProperty.call(state.partsDataByAddress, part.address)
            );

            return Promise.all(
                notLoaded.map(async part => {
                    const blob = await service.fetch(imapUid, part.address, part.encoding, part.mime, part.charset);
                    let converted =
                        MimeType.isHtml(part) || MimeType.isText(part)
                            ? await convertAsText(blob, part)
                            : await convertToBase64(blob);
                    commit(SET_ACTIVE_MESSAGE_PART_DATA, { data: converted, address: part.address });
                    return Promise.resolve();
                })
            );
        }
    },

    state: {
        partsDataByAddress: {}
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
