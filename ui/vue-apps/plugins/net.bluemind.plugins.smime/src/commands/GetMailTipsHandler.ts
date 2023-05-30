import { MailTipContext } from "@bluemind/mailmessage.api";
import { mailTipUtils } from "@bluemind/mail";
import { hasToBeEncrypted, hasToBeSigned } from "../lib/helper";

const { MailTipTypes } = mailTipUtils;

export default function ({ context, message }: { context: MailTipContext; message: any }) {
    const toBeEncrypted = hasToBeEncrypted(message.headers);

    // corporate signatures will not be applied if message is encrypted or signed
    if (toBeEncrypted || hasToBeSigned(message.headers)) {
        const index = context.filter?.mailTips?.findIndex(tip => tip === MailTipTypes.SIGNATURE) as number;
        if (index > -1) {
            context.filter?.mailTips?.splice(index, 1);
        }
    }

    if (toBeEncrypted) {
        context.filter?.mailTips?.push(MailTipTypes.HAS_PUBLIC_KEY_CERTIFICATE);
    }
    return { context };
}
