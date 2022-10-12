import { CHECK_IF_ASSOCIATED, DISSOCIATE_CRYPTO_FILES } from "./actionTypes";
import { SMIME_AVAILABLE } from "./getterTypes";
import { IS_SW_AVAILABLE } from "../helper";
import { SET_LOADING, SET_SW_ERROR, SET_HAS_PRIVATE_KEY, SET_HAS_PUBLIC_CERT } from "./mutationTypes";

// FIXME: with service-worker global env
const SW_INTERNAL_API_PATH = "/service-worker-internal/";

export default {
    namespaced: true,
    state: {
        hasPrivateKey: false,
        hasPublicCert: false,
        loading: true,
        swError: false
    },
    getters: {
        [SMIME_AVAILABLE]: state => IS_SW_AVAILABLE && state.hasPublicCert && state.hasPrivateKey
    },
    actions: {
        async [CHECK_IF_ASSOCIATED]({ commit }) {
            if (IS_SW_AVAILABLE) {
                commit(SET_LOADING, true);
                try {
                    const url = new URL(SW_INTERNAL_API_PATH + "smime", self.location.origin);
                    const options = { method: "GET" };
                    const response = await fetch(url, options);
                    const areAssociated = await response.json();
                    commit(SET_HAS_PUBLIC_CERT, areAssociated.publicCert);
                    commit(SET_HAS_PRIVATE_KEY, areAssociated.privateKey);
                    commit(SET_SW_ERROR, false);
                } catch {
                    commit(SET_SW_ERROR, true);
                } finally {
                    commit(SET_LOADING, false);
                }
            }
        },
        async [DISSOCIATE_CRYPTO_FILES]({ commit }) {
            commit(SET_LOADING, true);
            try {
                const url = new URL(SW_INTERNAL_API_PATH + "smime", self.location.origin);
                const options = { method: "DELETE" };
                await fetch(url, options);
                commit(SET_HAS_PUBLIC_CERT, false);
                commit(SET_HAS_PRIVATE_KEY, false);
                commit(SET_SW_ERROR, false);
            } catch {
                commit(SET_SW_ERROR, true);
            } finally {
                commit(SET_LOADING, false);
            }
        }
    },
    mutations: {
        [SET_LOADING]: (state, loading) => {
            state.loading = loading;
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
