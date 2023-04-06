<script>
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
        cannotEncrypt() {
            return this.contact && this.$store.state.mail.smime.missingCertificates.includes(this.contact.address);
        }
    },
    render() {
        let newAttrs = {};
        if (!this.invalid) {
            newAttrs = {
                invalid: this.cannotEncrypt,
                invalidIcon: this.cannotEncrypt ? "lock-slash" : null,
                invalidTooltip: this.cannotEncrypt ? this.$t("smime.mailapp.composer.invalid_recipient") : null
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
