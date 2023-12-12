import { inject } from "@bluemind/inject";

export const MailTipTypes = {
    SIGNATURE: "Signature",
    HAS_PUBLIC_KEY_CERTIFICATE: "HasPublicKeyCertificate"
};

interface IMessage {
    from: IRecipient;
    to: IRecipient[];
    cc: IRecipient[];
    bcc: IRecipient[];
    subject: any;
}

interface IRecipient {
    address: string;
    dn: string;
}

export function getMailTipContext(message: IMessage) {
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

function getRecipients(message: IMessage) {
    return message.to
        .map(adaptor("TO"))
        .concat(message.cc.map(adaptor("CC")))
        .concat(message.bcc.map(adaptor("BCC")));

    function adaptor(type: any) {
        return ({ address, dn }: { address: any; dn: any }) => ({
            email: address,
            name: dn,
            recipientType: type,
            addressType: "SMTP"
        });
    }
}

export default {
    getMailTipContext,
    MailTipTypes
};
