import { mailTipUtils } from "@bluemind/mail";
import addCertificate from "../../lib/addCertificate";
import { ADD_CERTIFICATE, DISPLAY_UNTRUSTED, RESET_MISSING_CERTIFICATES } from "./types";
import { smimeErrorMsgRegex, SMIME_ENCRYPTION_ERROR_PREFIX, SMIME_SIGNATURE_ERROR_PREFIX } from "../../lib/constants";
import { withAlert } from "./withAlertSmime";

const { MailTipTypes } = mailTipUtils;

export default {
    state: {
        displayUntrusted: [],
        missingCertificates: [],
        encryptError: null,
        signError: null
    },
    actions: {
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
