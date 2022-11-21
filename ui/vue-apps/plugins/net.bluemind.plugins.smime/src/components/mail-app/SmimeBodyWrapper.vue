<script>
import { BmButton } from "@bluemind/styleguide";
import { isSigned, isVerified } from "../../lib/helper";
import untrustedIllustration from "../../../assets/mail-app-untrusted.png";

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
        return { isVerified, isSigned, untrustedIllustration };
    },
    computed: {
        forceDisplay() {
            return this.$store.state.smime.displayUntrusted.indexOf(this.message.key) !== -1;
        }
    },
    render(h) {
        if (!this.forceDisplay && isSigned(this.message.headers) && !isVerified(this.message.headers)) {
            const imgDiv = h("div", {}, [h("img", { attrs: { src: untrustedIllustration } })]);
            //  FIXME doc url
            const button = h(
                "bm-button",
                { props: { variant: "link" }, class: "mt-6", attrs: { target: "_blank" } },
                "Que se passe-t-il ?" //  FIXME i18n
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
@import "~@bluemind/styleguide/css/_variables";

.smime-body-wrapper {
    display: flex;
    flex-direction: column;
    align-items: center;
    background-color: $neutral-bg-lo1;
}
</style>
