<script>
import { mapGetters } from "vuex";
import { hasToBeEncrypted } from "../../lib/helper";
export default {
    name: "ContactWithCertificate",
    props: {
        contact: {
            type: Object,
            default: null
        },
        invalid: {
            type: Boolean,
            default: false
        }
    },
    computed: {
        ...mapGetters("mail", {
            ACTIVE_MESSAGE: "ACTIVE_MESSAGE"
        }),
        cannotEncrypt() {
            return this.contact && this.$store.state.mail.smime.missingCertificates.includes(this.contact.address);
        },
        hasToBeEncrypted() {
            return hasToBeEncrypted(this.ACTIVE_MESSAGE.headers);
        }
    },
    render() {
        let newAttrs = {};
        if (!this.invalid) {
            const invalid = this.cannotEncrypt && this.hasToBeEncrypted;
            newAttrs = {
                invalid,
                invalidIcon: invalid ? "lock-slash" : null,
                invalidTooltip: invalid ? this.$t("smime.mailapp.composer.invalid_recipient") : null
            };
        }
        return this.$scopedSlots.default({
            ...this.$attrs,
            ...this.$props,
            ...newAttrs
        });
    }
};
</script>
