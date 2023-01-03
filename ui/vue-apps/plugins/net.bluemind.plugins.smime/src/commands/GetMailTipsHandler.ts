import { MailTipContext } from "@bluemind/mailmessage.api";
import { mailTipUtils } from "@bluemind/mail";

const { MailTipTypes } = mailTipUtils;

export default function ({ context }: { context: MailTipContext }) {
    context.filter?.mailTips?.push(MailTipTypes.HAS_PUBLIC_KEY_CERTIFICATE);
    return { context };
}
