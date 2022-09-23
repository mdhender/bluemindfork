import escape from "lodash.escape";
import escapeRegExp from "lodash.escaperegexp";
import apiMessages from "~/store/api/apiMessages";

export default {
    methods: {
        async showSource(message) {
            const blob = await apiMessages.fetchComplete(message);
            const text = await blob.text();
            const w = window.open();
            w.document.write(`<html lang="en"><header/><body><pre>${escape(text)}</pre></body></html>`);
            w.document.title = this.createEmlName(message);
        },
        async downloadEml(message) {
            const blob = await apiMessages.fetchComplete(message);
            const objectURL = URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = objectURL;
            a.download = this.createEmlName(message);
            a.click();
            URL.revokeObjectURL(objectURL);
        },
        createEmlName(message) {
            // @see  https://www.mtu.edu/umc/services/websites/writing/characters-avoid/
            const maxChars = 31;
            const extension = ".eml";
            const illegalChars = "#<>$+%!`&*'|{}?\"=/\\:@";
            const illegalCharsRegex = new RegExp(`[${escapeRegExp(illegalChars)}\\s]+`, "g");
            let subject = message.subject || this.$t("mail.viewer.no.subject");
            if (subject.length > maxChars - extension.length) {
                subject = subject.substring(0, maxChars - extension.length);
            }
            const replacementChar = "-";
            return `${subject.replaceAll(illegalCharsRegex, replacementChar)}${extension}`;
        }
    }
};
