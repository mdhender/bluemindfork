import { DN_EMAIL_REGEX, EMAIL_REGEX } from "./EmailValidator";

const SEPARATORS = ",;";
const DN_LIMITS = [
    { start: "<", end: ">" },
    { start: '"', end: '"' },
    { start: "'", end: "'" },
    { start: "", end: "" }
];
const dnStringReFn = limit =>
    `[\\s${SEPARATORS}]${limit.start ? "*" : "+"}${limit.start}([^${limit.start}${limit.end}]+)${
        limit.end
    }[\\s${SEPARATORS}]${limit.end ? "*" : "+"}`;
let dnStringRegex = DN_LIMITS.reduce((res, limit) => (res ? `${res}|` : "") + dnStringReFn(limit), "");
dnStringRegex = `${DN_LIMITS.length > 1 ? "(?:" : ""}${dnStringRegex}${DN_LIMITS.length > 1 ? ")" : ""}`;
const DN_REGEX = new RegExp(dnStringRegex);

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
    },
    extractEmails(str) {
        const results = [];
        const addressRegex = new RegExp(EMAIL_REGEX, "gm");
        let res,
            lastIndex = 0;
        while ((res = addressRegex.exec(str)) !== null) {
            const possibleDN = str.substring(lastIndex, res.index);
            const match = possibleDN?.match(DN_REGEX);
            const foundValue = DN_LIMITS.reduce((res, cur, index) => (res ? res : match && match[index + 1]), false);
            const dn = match?.length ? foundValue?.trim() : "";
            const address = res[0];
            lastIndex = res.index + address.length;
            results.push({ address, dn });
        }
        return results;
    }
};
