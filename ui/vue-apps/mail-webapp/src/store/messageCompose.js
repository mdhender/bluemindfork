import { inject } from "@bluemind/inject";
import { signatureUtils } from "@bluemind/mail";
import { CHECK_CORPORATE_SIGNATURE, LOAD_MAX_MESSAGE_SIZE } from "~/actions";
import {
    MAX_MESSAGE_SIZE_EXCEEDED,
    RESET_COMPOSER,
    SET_CORPORATE_SIGNATURE,
    SET_PERSONAL_SIGNATURE,
    SET_DISCLAIMER,
    SET_DRAFT_COLLAPSED_CONTENT,
    SET_DRAFT_EDITOR_CONTENT,
    SET_MAX_MESSAGE_SIZE,
    SET_SAVED_INLINE_IMAGES,
    SHOW_SENDER,
    UNSET_CORPORATE_SIGNATURE
} from "~/mutations";
import { IS_SENDER_SHOWN } from "~/getters";
import templateChooser from "./templateChooser";

const { isCorporateSignature, isDisclaimer } = signatureUtils;

export default {
    mutations: {
        [MAX_MESSAGE_SIZE_EXCEEDED]: (state, hasExceeded) => {
            state.maxMessageSizeExceeded = hasExceeded;
        },
        [RESET_COMPOSER]: state => {
            state.disclaimer = null;
            state.corporateSignature = null;
            state.personalSignature = null;
            state.editorContent = "";
            state.collapsedContent = null;
            state.inlineImagesSaved = [];
            state.isSenderShown = false;

            state.maxMessageSizeExceeded = false;
        },
        [SET_CORPORATE_SIGNATURE]: (state, mailTip) => {
            if (!state.corporateSignature || state.corporateSignature.uid !== mailTip.uid) {
                state.corporateSignature = mailTip;
            }
        },
        [SET_PERSONAL_SIGNATURE]: (state, signature) => {
            state.personalSignature = signature;
        },
        [SET_DISCLAIMER]: (state, mailTip) => {
            if (!mailTip || !state.disclaimer || state.disclaimer.uid !== mailTip.uid) {
                state.disclaimer = mailTip;
            }
        },
        [SET_DRAFT_EDITOR_CONTENT]: (state, content) => {
            state.editorContent = content;
        },
        [SET_DRAFT_COLLAPSED_CONTENT]: (state, collapsed) => {
            state.collapsedContent = collapsed;
        },
        [SET_SAVED_INLINE_IMAGES]: (state, inlineImages) => {
            state.inlineImagesSaved = inlineImages;
        },
        [SET_MAX_MESSAGE_SIZE](state, size) {
            state.maxMessageSize = size;
        },
        [SHOW_SENDER]: (state, value) => {
            state.isSenderShown = value;
        },
        [UNSET_CORPORATE_SIGNATURE]: state => {
            state.corporateSignature = null;
        }
    },

    actions: {
        async [LOAD_MAX_MESSAGE_SIZE]({ commit }, userId) {
            const { messageMaxSize } = await inject("MailboxesPersistence").getMailboxConfig(userId);
            // take into account the email base64 encoding : 33% more space
            commit(SET_MAX_MESSAGE_SIZE, messageMaxSize / 1.33);
        },
        async [CHECK_CORPORATE_SIGNATURE]({ commit }, { message }) {
            const context = getMailTipContext(message);
            const mailTips = await inject("MailTipPersistence").getMailTips(context);

            if (mailTips.length > 0) {
                const matchingTips = mailTips[0].matchingTips;

                const disclaimer = matchingTips.find(isDisclaimer);
                commit(SET_DISCLAIMER, disclaimer ? JSON.parse(disclaimer.value) : null);

                const corporateSignature = matchingTips.find(isCorporateSignature);
                if (corporateSignature) {
                    commit(SET_CORPORATE_SIGNATURE, JSON.parse(corporateSignature.value));
                } else {
                    commit(UNSET_CORPORATE_SIGNATURE);
                }
            } else {
                commit(SET_DISCLAIMER, null);
                commit(UNSET_CORPORATE_SIGNATURE);
            }
        }
    },

    getters: {
        [IS_SENDER_SHOWN]: state => userSettings => state.isSenderShown || userSettings.always_show_from === "true"
    },

    state: {
        disclaimer: null,
        corporateSignature: null,
        personalSignature: null,
        editorContent: "",
        collapsedContent: null,
        inlineImagesSaved: [],
        maxMessageSize: 0, // FIXME: it's a cross-composer data, it must be moved in another store
        isSenderShown: false,
        maxMessageSizeExceeded: false,
        showFormattingToolbar: false,
        synced: ["showFormattingToolbar"]
    },
    modules: {
        templateChooser
    }
};

function getMailTipContext(message) {
    return {
        messageContext: {
            fromIdentity: {
                sender: inject("UserSession").defaultEmail,
                from: message.from.address
            },
            messageClass: "Mail",
            recipients: getRecipients(message),
            subject: message.subject
        },
        filter: {
            filterType: "INCLUDE",
            mailTips: ["Signature"]
        }
    };
}

function getRecipients(message) {
    const adaptor = type => ({ address, dn }) => ({
        email: address,
        name: dn,
        recipientType: type,
        addressType: "SMTP"
    });
    return message.to
        .map(adaptor("TO"))
        .concat(message.cc.map(adaptor("CC")))
        .concat(message.bcc.map(adaptor("BCC")));
}
