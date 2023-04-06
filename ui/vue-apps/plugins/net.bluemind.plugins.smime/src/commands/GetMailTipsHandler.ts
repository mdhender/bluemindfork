import { MailTipContext } from "@bluemind/mailmessage.api";
import { mailTipUtils } from "@bluemind/mail";
import { hasToBeEncrypted } from "../lib/helper";

const { MailTipTypes } = mailTipUtils;

export default function ({ context, message }: { context: MailTipContext; message: any }) {
    if (hasToBeEncrypted(message.headers)) {
        context.filter?.mailTips?.push(MailTipTypes.HAS_PUBLIC_KEY_CERTIFICATE);
    }
    return { context };
}
