import { Commit } from "vuex";
import { PKIStatus, SMIME_INTERNAL_API_URL } from "../../lib/constants";
import {
    CHECK_IF_ASSOCIATED,
    DISSOCIATE_CRYPTO_FILES,
    SET_HAS_PRIVATE_KEY,
    SET_HAS_PUBLIC_CERT,
    SET_SW_AVAILABLE,
    SET_SW_ERROR,
    SMIME_AVAILABLE
} from "./types";

const actions = {
    async [CHECK_IF_ASSOCIATED]({ commit, state }: { commit: Commit; state: SMimeRootState }) {
        if (state.isServiceWorkerAvailable) {
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
    async [DISSOCIATE_CRYPTO_FILES]({ commit }: { commit: Commit }) {
        try {
            await fetch(SMIME_INTERNAL_API_URL, { method: "DELETE" });
            commit(SET_HAS_PUBLIC_CERT, false);
            commit(SET_HAS_PRIVATE_KEY, false);
            commit(SET_SW_ERROR, false);
        } catch {
            commit(SET_SW_ERROR, true);
        }
    }
};

const mutations = {
    [SET_SW_AVAILABLE]: (state: SMimeRootState, isAvailable: boolean) => {
        state.isServiceWorkerAvailable = isAvailable;
    },
    [SET_SW_ERROR]: (state: SMimeRootState, swError: boolean) => {
        state.swError = swError;
    },
    [SET_HAS_PRIVATE_KEY]: (state: SMimeRootState, hasPrivateKey: boolean) => {
        state.hasPrivateKey = hasPrivateKey;
    },
    [SET_HAS_PUBLIC_CERT]: (state: SMimeRootState, hasPublicCert: boolean) => {
        state.hasPublicCert = hasPublicCert;
    }
};

type SMimeRootState = {
    isServiceWorkerAvailable: boolean;
    hasPrivateKey: boolean;
    hasPublicCert: boolean;
    swError: boolean;
};

export default {
    namespaced: true,
    actions,
    getters: {
        [SMIME_AVAILABLE]: (state: SMimeRootState) =>
            state.isServiceWorkerAvailable && state.hasPublicCert && state.hasPrivateKey
    },
    mutations,
    state: {
        isServiceWorkerAvailable: !!navigator.serviceWorker?.controller,

        // preferences
        hasPrivateKey: false,
        hasPublicCert: false,
        swError: false
    }
};
