// This regex has been copied from EmailValidator,
// the only difference is that characters ^ and $
// have been removed from beginning and end of the regex.
/*eslint no-useless-escape: "off"*/
/*eslint max-len: ["error", { "ignoreRegExpLiterals": true }]*/
const EMAIL_REGEX = /(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))/;

export default {
    extractEmail(str) {
        const res = EMAIL_REGEX.exec(str);
        if (res !== null) {
            return res[0];
        }
        return null;
    }
};
