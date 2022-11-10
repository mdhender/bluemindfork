import { DN_EMAIL_REGEX, EMAIL_REGEX } from "./EmailValidator";

export default {
    extractEmail(str) {
        const res = EMAIL_REGEX.exec(str);
        if (res !== null) {
            return res[0];
        }
        return null;
    },
    extractDN(str) {
        const match = str?.match(DN_EMAIL_REGEX);
        return match ? match[1] : str;
    }
};
