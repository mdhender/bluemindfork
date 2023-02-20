<script>
import { pki } from "node-forge";
import { MimeType } from "@bluemind/email";
import { INFO, REMOVE } from "@bluemind/alert.store";

const alert = {
    name: "smime.ADD_CERTIFICATE",
    uid: "SMIME_ADD_CERTIFICATE",
    payload: null
};
const options = { area: "preview-right-panel", renderer: "AddCertificateAlert", dismissible: false };

export default {
    name: "CertificateViewer",
    props: {
        next: {
            type: Function,
            required: true
        },
        file: {
            type: Object,
            required: true
        }
    },
    data() {
        return { renderer: () => "" };
    },
    watch: {
        file: {
            async handler() {
                if (MimeType.X509_CERT === this.file.mime) {
                    try {
                        let certificate, text, ownerName, ownerEmail;
                        certificate = this.file.extra?.certificate;
                        text = this.file.extra?.pem || pki.certificateToPem(certificate);
                        ownerName = this.file.extra.ownerName;
                        ownerEmail = this.file.extra.ownerEmail;

                        const newAlert = { ...alert, payload: { certificate, pem: text, ownerName, ownerEmail } };

                        this.$store.dispatch(`alert/${INFO}`, {
                            alert: newAlert,
                            options
                        });
                        this.renderer = h => h("div", { class: "certificate-viewer bg-surface" }, text);
                    } catch {
                        this.renderer = () => this.next();
                    }
                } else {
                    this.$store.dispatch(`alert/${REMOVE}`, alert);
                    this.renderer = () => this.next();
                }
            },
            immediate: true
        }
    },
    destroyed() {
        this.$store.dispatch(`alert/${REMOVE}`, alert);
    },
    render(h) {
        return this.renderer(h);
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

.certificate-viewer {
    display: flex;
    margin: 0 $sp-8;
    padding: $sp-6;
    min-height: 100%;
}
</style>
