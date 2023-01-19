import { mapActions } from "vuex";
import { hasSignatureHeader, isVerified } from "../../lib/helper";

export default {
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            alert: { name: "smime.untrusted_sender", uid: "SMIME_UNTRUSTED_SENDER", payload: this.message.key },
            options: { area: "right-panel", icon: "lock", renderer: "UntrustedSenderAlert" }
        };
    },
    watch: {
        "message.key": {
            handler: function () {
                if (hasSignatureHeader(this.message.headers) && !isVerified(this.message.headers)) {
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
