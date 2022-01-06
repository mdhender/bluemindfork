import { inject } from "@bluemind/inject";
import { CHECK_CORPORATE_SIGNATURE, LOAD_MAX_MESSAGE_SIZE } from "~/actions";
import {
    RESET_PENDING_ATTACHMENTS,
    SET_CORPORATE_SIGNATURE,
    SET_DISCLAIMER,
    SET_DRAFT_COLLAPSED_CONTENT,
    SET_DRAFT_EDITOR_CONTENT,
    SET_MAX_MESSAGE_SIZE,
    SET_PENDING_ATTACHMENTS,
    SET_SAVED_INLINE_IMAGES,
    SHOW_SENDER
} from "~/mutations";
import { IS_SENDER_SHOWN } from "~/getters";
import templateChooser from "./templateChooser";

export default {
    mutations: {
        [RESET_PENDING_ATTACHMENTS]: state => {
            state.pendingAttachments = [];
        },
        [SET_CORPORATE_SIGNATURE]: (state, mailTip) => {
            state.corporateSignature = mailTip;
        },
        [SET_DISCLAIMER]: (state, mailTip) => {
            state.disclaimer = mailTip;
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
        [SET_PENDING_ATTACHMENTS]: (state, attachments) => {
            state.pendingAttachments = attachments;
        },
        [SET_MAX_MESSAGE_SIZE](state, size) {
            state.maxMessageSize = size;
        },
        [SHOW_SENDER]: (state, value) => (state.isSenderShown = value)
    },

    actions: {
        async [LOAD_MAX_MESSAGE_SIZE]({ commit }, userId) {
            const { messageMaxSize } = await inject("MailboxesPersistence").getMailboxConfig(userId);
            // take into account the email base64 encoding : 33% more space
            commit(SET_MAX_MESSAGE_SIZE, messageMaxSize / 1.33);
        },
        async [CHECK_CORPORATE_SIGNATURE]({ commit }, { message, selectedIdentity }) {
            commit(SET_DISCLAIMER, null);
            commit(SET_CORPORATE_SIGNATURE, null);

            const context = getMailTipContext(message, selectedIdentity);
            const mailTips = await inject("MailTipPersistence").getMailTips(context);
            if (mailTips.length > 0) {
                mailTips[0].matchingTips.forEach(tip => {
                    const desc = JSON.parse(tip.value);
                    if (tip.mailtipType === "Signature") {
                        if (desc.isDisclaimer) {
                            commit(SET_DISCLAIMER, desc);
                        } else {
                            commit(SET_CORPORATE_SIGNATURE, desc);
                        }
                    }
                });
            }
        }
    },

    getters: {
        [IS_SENDER_SHOWN]: state => userSettings => state.isSenderShown || !!userSettings.always_show_from
    },

    state: {
        disclaimer: null,
        corporateSignature: null,
        editorContent: "",
        collapsedContent: null,
        inlineImagesSaved: [],
        maxMessageSize: 0,
        pendingAttachments: [], // used only to store forwarded attachments when they are not uploaded
        isSenderShown: false
    },
    modules: {
        templateChooser
    }
};

function getMailTipContext(message, selectedIdentity) {
    return {
        messageContext: {
            fromIdentity: {
                sender: inject("UserSession").defaultEmail,
                from: selectedIdentity.email || inject("UserSession").defaultEmail // FIXME
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
