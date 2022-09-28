import escape from "lodash.escape";
import { messageUtils } from "@bluemind/mail";
import apiMessages from "~/store/api/apiMessages";

export default {
    methods: {
        async showSource(message) {
            const blob = await apiMessages.fetchComplete(message);
            const text = await blob.text();
            const w = window.open();
            w.document.write(`<html lang="en"><header/><body><pre>${escape(text)}</pre></body></html>`);
            w.document.title = messageUtils.createEmlName(message);
        },
        async downloadEml(message) {
            const blob = await apiMessages.fetchComplete(message);
            const objectURL = URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = objectURL;
            a.download = messageUtils.createEmlName(message, this.$t("mail.viewer.no.subject"));
            a.click();
            URL.revokeObjectURL(objectURL);
        }
    }
};
