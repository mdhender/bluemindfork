import store from "@bluemind/store";
import { inject } from "@bluemind/inject";
import { SET_MAIL_TIPS } from "~/mutations";
import { useCommand } from "@bluemind/command";

async function getMailTips({ context }) {
    if (context.filter.mailTips.length > 0) {
        const mailTips = await inject("MailTipPersistence").getMailTips(context);
        store.commit(`mail/${SET_MAIL_TIPS}`, mailTips);
    } else {
        store.commit(`mail/${SET_MAIL_TIPS}`, []);
    }
}

export default { commands: { getMailTips } };
export const useGetMailTipsCommand = () => useCommand("getMailTips", getMailTips);
