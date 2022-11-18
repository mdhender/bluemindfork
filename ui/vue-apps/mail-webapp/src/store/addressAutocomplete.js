import { SEND_MESSAGE } from "~/actions";
import { ADDRESS_AUTOCOMPLETE } from "~/getters";
import { ADD_ADDRESS_WEIGHT } from "~/mutations";

export default {
    state: {
        addressWeights: {},
        synced: { addressWeights: ADD_ADDRESS_WEIGHT }
    },

    mutations: {
        [ADD_ADDRESS_WEIGHT]: (state, { address, weight }) => {
            state.addressWeights[address] = (state.addressWeights[address] || 0) + weight;
        }
    },

    getters: {
        [ADDRESS_AUTOCOMPLETE]: state => {
            const addressAutoComplete = { sortedAddresses: [], excludedAddresses: [] };
            Object.keys(state.addressWeights).forEach(address => {
                state.addressWeights[address] > 0
                    ? addressAutoComplete.sortedAddresses.push(address)
                    : addressAutoComplete.excludedAddresses.push(address);
            });
            addressAutoComplete.sortedAddresses.sort((a, b) => state.addressWeights[b] - state.addressWeights[a]);
            return addressAutoComplete;
        }
    },

    actions: {
        [SEND_MESSAGE]: (store, { draft }) => {
            const sentAddresses = new Set(
                draft.to
                    .concat(draft.cc)
                    .concat(draft.bcc)
                    .map(({ address }) => address)
            );
            const weight = new Date().getTime();
            sentAddresses.forEach(address => store.commit(ADD_ADDRESS_WEIGHT, { address, weight }));
        }
    }
};
