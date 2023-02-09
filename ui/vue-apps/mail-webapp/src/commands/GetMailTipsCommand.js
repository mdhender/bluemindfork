import { inject } from "@bluemind/inject";
import { SET_MAIL_TIPS } from "~/mutations";

export default {
    commands: {
        async getMailTips({ context }) {
            if (context.filter.mailTips.length > 0) {
                const mailTips = await inject("MailTipPersistence").getMailTips(context);
                this.$store.commit(`mail/${SET_MAIL_TIPS}`, mailTips);
            }
        }
    }
};
