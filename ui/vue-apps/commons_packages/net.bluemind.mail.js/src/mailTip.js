import { inject } from "@bluemind/inject";

export const MailTipTypes = {
    SIGNATURE: "Signature",
    HAS_PUBLIC_KEY_CERTIFICATE: "HasPublicKeyCertificate"
};

export function getMailTipContext(message) {
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
            mailTips: []
        }
    };
}

function getRecipients(message) {
    const adaptor =
        type =>
        ({ address, dn }) => ({
            email: address ?? null,
            name: dn,
            recipientType: type,
            addressType: "SMTP"
        });
    return message.to
        .map(adaptor("TO"))
        .concat(message.cc.map(adaptor("CC")))
        .concat(message.bcc.map(adaptor("BCC")));
}

export default {
    getMailTipContext,
    MailTipTypes
};
