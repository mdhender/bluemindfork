<script>
import { mapGetters } from "vuex";
import { pki } from "node-forge";
import { MimeType } from "@bluemind/email";
import { base64ToArrayBuffer } from "@bluemind/arraybuffer";
import { getSignedDataEnvelope } from "../../service-worker/pkcs7/verify";

export default {
    name: "Pkcs7FileItem",
    props: {
        file: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            url: null,
            adaptedFile: null
        };
    },
    computed: {
        ...mapGetters("mail", ["ACTIVE_MESSAGE"])
    },
    watch: {
        url(_, old) {
            URL.revokeObjectURL(old);
        }
    },
    async created() {
        this.adaptedFile = this.file;
        if (this.file.mime === MimeType.PKCS_7_SIGNED_DATA) {
            await this.$store.dispatch("mail/FETCH_PART_DATA", {
                folderUid: this.ACTIVE_MESSAGE.folderRef.uid,
                imapUid: this.ACTIVE_MESSAGE.remoteRef.imapUid,
                parts: [this.file],
                messageKey: this.ACTIVE_MESSAGE.key
            });

            const certificate = extractCertificate(
                this.$store.state.mail.partsData.partsByMessageKey[this.ACTIVE_MESSAGE.key][this.file.address]
            );
            const subject = certificate.subject.attributes.pop();
            const name = subject?.value || "public_certificate";

            const pem = pki.certificateToPem(certificate);
            this.url = URL.createObjectURL(new File([pem], this.name));

            this.adaptedFile = {
                ...this.file,
                url: this.url,
                mime: MimeType.X509_CERT,
                name: `${name}.crt`
            };
        }
    },
    destroyed() {
        URL.revokeObjectURL(this.url);
    },
    render() {
        return this.$scopedSlots.default({ file: this.adaptedFile });
    }
};

function extractCertificate(base64Part) {
    const p7s = base64ToArrayBuffer(base64Part.split(",").pop());
    const envelope = getSignedDataEnvelope(p7s);
    return envelope.certificates[0];
}
</script>
