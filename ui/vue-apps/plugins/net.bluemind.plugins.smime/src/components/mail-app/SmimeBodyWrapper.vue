<script>
import { BmButton } from "@bluemind/ui-components";
import { isDecrypted, isEncrypted, isSigned, isVerified } from "../../lib/helper";
import untrustedIllustration from "../../../assets/mail-app-untrusted.png";
import undecryptedIllustration from "../../../assets/mail-app-undecrypted.png";

export default {
    name: "SmimeBodyWrapper",
    components: { BmButton },
    props: {
        message: {
            type: Object,
            required: true
        },
        next: {
            type: Function,
            required: true
        }
    },
    data() {
        return {
            untrustedIllustration,
            undecryptedIllustration
        };
    },
    computed: {
        forceDisplay() {
            return this.$store.state.smime.displayUntrusted.includes(this.message.key);
        },
        untrusted() {
            return isSigned(this.message.headers) && !isVerified(this.message.headers);
        },
        undecrypted() {
            return isEncrypted(this.message.headers) && !isDecrypted(this.message.headers);
        }
    },
    render(h) {
        const src = this.untrusted ? untrustedIllustration : undecryptedIllustration;
        const text = this.untrusted ? "Que se passe-t-il ?" : "Pourquoi je ne peux pas afficher ce message ?"; //  FIXME i18n

        if ((!this.forceDisplay && this.untrusted) || this.undecrypted) {
            const imgDiv = h("div", {}, [h("img", { attrs: { src } })]);
            //  FIXME doc url
            const button = h(
                "bm-button",
                { props: { variant: "link" }, class: "mt-6", attrs: { target: "_blank" } },
                text
            );
            const children = [imgDiv, button];
            return h("div", { class: "smime-body-wrapper py-8" }, [children]);
        } else {
            return this.next();
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/_variables";

.smime-body-wrapper {
    display: flex;
    flex-direction: column;
    align-items: center;
    background-color: $neutral-bg-lo1;
}
</style>
