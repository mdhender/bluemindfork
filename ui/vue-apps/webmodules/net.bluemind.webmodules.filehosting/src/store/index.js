import Vue from "vue";
import actions from "./actions";

const mutations = {
    // Listeners
    REMOVE_ATTACHMENT(state, { messageKey, address }) {
        if (state.values[messageKey] && state.values[messageKey][address]) {
            const attachments = state.values[messageKey];
            delete attachments[address];
        }
    },
    ADD_MESSAGES(state, { messages }) {
        messages.forEach(({ key, attachments }) => {
            attachments?.forEach(attachment => addAttachment(state, { messageKey: key, attachment }));
        });
    },
    ADD_ATTACHMENT: addAttachment,
    SET_ATTACHMENT_ADDRESS(state, { messageKey, oldAddress, address }) {
        if (state.values[messageKey] && state.values[messageKey][oldAddress]) {
            let infos = state.values[messageKey][oldAddress];
            Vue.delete(state.values[messageKey], oldAddress);
            Vue.set(state.values[messageKey], address, infos);
        }
    },
    SET_ATTACHMENT_HEADERS(state, { messageKey, address, headers }) {
        addAttachment(state, { messageKey, attachment: { address, headers } });
    },
    SET_MESSAGE_TMP_ADDRESSES(state, { key, attachments }) {
        Vue.delete(state.values, key);
        attachments.forEach(attachment => addAttachment(state, { messageKey: key, attachment }));
    }
};
const getters = {
    GET_FH_ATTACHMENT(state) {
        return ({ key }, { address }) => state.values[key]?.[address];
    }
};

export default {
    namespaced: false,
    state: { values: {} },
    mutations,
    actions,
    getters
};

function addAttachment(state, { messageKey, attachment: { address, headers } }) {
    let header =
        headers.find(header => header.name.toLowerCase() === "x-bm-disposition") ||
        headers.find(header => header.name.toLowerCase() === "x-mozilla-cloud-part");
    if (header) {
        const data = extractFileHostingInfos(header);
        if (!state.values[messageKey]) {
            Vue.set(state.values, messageKey, { [address]: data });
        } else {
            Vue.set(state.values[messageKey], address, data);
        }
    }
}

function extractFileHostingInfos(header) {
    let { name, ...headers } = Object.fromEntries(
        header.values[0]
            .split(";")
            .slice(1)
            .map(s => s.match(/ *([^=]*)=(.*)/).slice(1, 3))
    );

    return {
        fileName: name,
        ...headers
    };
}
