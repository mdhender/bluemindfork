import { CHECK_IF_ASSOCIATED, DISSOCIATE_CRYPTO_FILES } from "./actionTypes";
import { SMIME_AVAILABLE } from "./getterTypes";
import { DISPLAY_UNTRUSTED, SET_SW_ERROR, SET_HAS_PRIVATE_KEY, SET_HAS_PUBLIC_CERT } from "./mutationTypes";
import { IS_SW_AVAILABLE, SMIME_INTERNAL_API_URL, PKIStatus } from "../lib/constants";

export default {
    namespaced: true,
    state: {
        // preferences
        hasPrivateKey: false,
        hasPublicCert: false,
        swError: false,

        // mail-app
        displayUntrusted: []
    },
    getters: {
        [SMIME_AVAILABLE]: state => IS_SW_AVAILABLE && state.hasPublicCert && state.hasPrivateKey
    },
    actions: {
        async [CHECK_IF_ASSOCIATED]({ commit }) {
            if (IS_SW_AVAILABLE) {
                try {
                    const response = await fetch(SMIME_INTERNAL_API_URL, { method: "GET" });
                    const status = await response.json();
                    commit(SET_HAS_PUBLIC_CERT, Boolean(status & PKIStatus.CERTIFICATE_OK));
                    commit(SET_HAS_PRIVATE_KEY, Boolean(status & PKIStatus.PRIVATE_KEY_OK));
                    commit(SET_SW_ERROR, false);
                } catch {
                    commit(SET_SW_ERROR, true);
                }
            }
        },
        async [DISSOCIATE_CRYPTO_FILES]({ commit }) {
            try {
                await fetch(SMIME_INTERNAL_API_URL, { method: "DELETE" });
                commit(SET_HAS_PUBLIC_CERT, false);
                commit(SET_HAS_PRIVATE_KEY, false);
                commit(SET_SW_ERROR, false);
            } catch {
                commit(SET_SW_ERROR, true);
            }
        }
    },
    mutations: {
        [DISPLAY_UNTRUSTED]: (state, messageKey) => {
            state.displayUntrusted.push(messageKey);
        },
        [SET_SW_ERROR]: (state, swError) => {
            state.swError = swError;
        },
        [SET_HAS_PRIVATE_KEY]: (state, hasPrivateKey) => {
            state.hasPrivateKey = hasPrivateKey;
        },
        [SET_HAS_PUBLIC_CERT]: (state, hasPublicCert) => {
            state.hasPublicCert = hasPublicCert;
        }
    }
};
