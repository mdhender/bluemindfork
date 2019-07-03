/*eslint no-useless-escape: "off"*/
/*eslint max-len: ["error", { "ignoreRegExpLiterals": true }]*/
const EMAIL_REGEX = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;

export default {
    validateAddress(email) {
        return EMAIL_REGEX.test(email);
    }
};