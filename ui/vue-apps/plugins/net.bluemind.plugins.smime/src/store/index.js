import { mailTipUtils } from "@bluemind/mail";
import { ADD_CERTIFICATE, CHECK_IF_ASSOCIATED, DISSOCIATE_CRYPTO_FILES } from "./actionTypes";
import { SMIME_AVAILABLE } from "./getterTypes";
import addCertificate from "../lib/addCertificate";
import {
    DISPLAY_UNTRUSTED,
    RESET_MISSING_CERTIFICATES,
    SET_SW_AVAILABLE,
    SET_SW_ERROR,
    SET_HAS_PRIVATE_KEY,
    SET_HAS_PUBLIC_CERT
} from "./mutationTypes";
import {
    PKIStatus,
    smimeErrorMsgRegex,
    SMIME_ENCRYPTION_ERROR_PREFIX,
    SMIME_INTERNAL_API_URL,
    SMIME_SIGNATURE_ERROR_PREFIX
} from "../lib/constants";
import { withAlert } from "./withAlertSmime";

const { MailTipTypes } = mailTipUtils;

export default {
    namespaced: false,
    state: {
        isServiceWorkerAvailable: !!navigator.serviceWorker?.controller,

        // preferences
        hasPrivateKey: false,
        hasPublicCert: false,
        swError: false,

        // mail-app
        displayUntrusted: [],
        missingCertificates: [],
        encryptError: null,
        signError: null
    },
    getters: {
        [SMIME_AVAILABLE]: state => state.isServiceWorkerAvailable && state.hasPublicCert && state.hasPrivateKey
    },
    actions: {
        async [CHECK_IF_ASSOCIATED]({ commit, state }) {
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
        async [DISSOCIATE_CRYPTO_FILES]({ commit }) {
            try {
                await fetch(SMIME_INTERNAL_API_URL, { method: "DELETE" });
                commit(SET_HAS_PUBLIC_CERT, false);
                commit(SET_HAS_PRIVATE_KEY, false);
                commit(SET_SW_ERROR, false);
            } catch {
                commit(SET_SW_ERROR, true);
            }
        },
        [ADD_CERTIFICATE](store, { pem, dn, email }) {
            const add = (_, { pem, dn, email }) => addCertificate(pem, dn, email);
            const execute = withAlert(add, ADD_CERTIFICATE);
            return execute(store, { pem, dn, email });
        }
    },
    mutations: {
        [DISPLAY_UNTRUSTED]: (state, messageKeys) => {
            state.displayUntrusted.push(...messageKeys);
        },
        [SET_SW_AVAILABLE]: (state, isAvailable) => {
            state.isServiceWorkerAvailable = isAvailable;
        },
        [SET_SW_ERROR]: (state, swError) => {
            state.swError = swError;
        },
        [SET_HAS_PRIVATE_KEY]: (state, hasPrivateKey) => {
            state.hasPrivateKey = hasPrivateKey;
        },
        [SET_HAS_PUBLIC_CERT]: (state, hasPublicCert) => {
            state.hasPublicCert = hasPublicCert;
        },
        [RESET_MISSING_CERTIFICATES]: state => {
            state.missingCertificates = [];
        },

        // Listeners
        SET_MAIL_TIPS: (state, mailTips) => {
            const cannotEncrypt = tip =>
                tip.mailtipType === MailTipTypes.HAS_PUBLIC_KEY_CERTIFICATE && tip.value === "false";
            const missingCertificates = Object.values(mailTips).flatMap(({ matchingTips, forRecipient }) => {
                const cannotEncryptTip = matchingTips.some(cannotEncrypt);
                return cannotEncryptTip && forRecipient?.email ? forRecipient.email : [];
            });
            state.missingCertificates = missingCertificates;
        },
        SET_SAVE_ERROR: (state, error) => {
            state.encryptError = null;
            state.signError = null;

            if (error && error.message && smimeErrorMsgRegex.test(error.message)) {
                const [errorType, errorCode] = error.message.match(smimeErrorMsgRegex).splice(1);
                const code = parseInt(errorCode) ? parseInt(errorCode) : null;
                if (errorType === SMIME_ENCRYPTION_ERROR_PREFIX) {
                    state.encryptError = code;
                } else if (errorType === SMIME_SIGNATURE_ERROR_PREFIX) {
                    state.signError = code;
                }
            }
        }
    }
};
