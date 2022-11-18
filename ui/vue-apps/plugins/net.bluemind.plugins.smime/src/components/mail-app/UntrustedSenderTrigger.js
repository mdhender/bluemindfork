import { mapActions } from "vuex";
import { CRYPTO_HEADERS, SIGNED_HEADER_NAME } from "../../lib/constants";

export default {
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            alert: { name: "smime.untrusted_sender", uid: "SMIME_UNTRUSTED_SENDER" },
            options: { area: "right-panel", icon: "lock", renderer: "UntrustedSenderAlert" }
        };
    },
    watch: {
        "message.key": {
            handler: function () {
                if (isSigned(this.message.headers) && !isVerified(this.message.headers)) {
                    this.WARNING({ alert: this.alert, options: this.options });
                } else {
                    this.REMOVE(this.alert);
                }
            },
            immediate: true
        }
    },
    destroyed() {
        this.REMOVE(this.alert);
    },
    methods: {
        ...mapActions("alert", ["REMOVE", "WARNING"])
    },
    render() {
        return "";
    }
};

function isVerified(headers) {
    const cryptoHeader = headers.find(header => header.name === SIGNED_HEADER_NAME);
    return cryptoHeader?.values.find(value => value === CRYPTO_HEADERS.VERIFIED);
}

function isSigned(headers) {
    const cryptoHeader = headers.find(header => header.name === SIGNED_HEADER_NAME);
    return !!cryptoHeader;
}
