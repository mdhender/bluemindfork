/*eslint no-useless-escape: "off"*/
/*eslint max-len: ["error", { "ignoreRegExpLiterals": true }]*/
export const EMAIL_REGEX =
    /(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))/;
const EMAIL_VALIDATION_REGEX = new RegExp(`^${EMAIL_REGEX.source}$`);
export const DN_EMAIL_REGEX =
    /(?:\s*"?([^"]*?)"?\s*[\s<]+)?(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))>?/;

export default {
    validateAddress(email) {
        return EMAIL_VALIDATION_REGEX.test(email);
    },
    validateDnAndAddress(value) {
        return DN_EMAIL_REGEX.test(value);
    }
};
