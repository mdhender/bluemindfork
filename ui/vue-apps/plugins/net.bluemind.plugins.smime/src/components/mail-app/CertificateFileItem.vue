<script>
import { pki } from "node-forge";
import { MimeType } from "@bluemind/email";
import { base64ToArrayBuffer } from "@bluemind/arraybuffer";
import { getSignedDataEnvelope } from "../../lib/envelope";

export default {
    name: "CertificateFileItem",
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
    watch: {
        url(_, old) {
            URL.revokeObjectURL(old);
        },
        file: {
            async handler(newFile) {
                this.adaptedFile = this.file;
                try {
                    if (canAdapt(this.file)) {
                        const messageKey = this.file.key.split(":")[1];
                        const message = this.$store.state.mail.conversations.messages[messageKey];
                        await this.$store.dispatch("mail/FETCH_PART_DATA", {
                            folderUid: message.folderRef.uid,
                            imapUid: message.remoteRef.imapUid,
                            parts: [newFile],
                            messageKey
                        });
                        const partData =
                            this.$store.state.mail.partsData.partsByMessageKey[messageKey][newFile.address];
                        const { certificate, pem } = await this.certificateFromBase64(partData);
                        const { email, name } = extractCertificateInfos(certificate);
                        this.url = URL.createObjectURL(new File([pem], email));

                        this.adaptedFile = {
                            ...newFile,
                            url: this.url,
                            mime: MimeType.X509_CERT,
                            name: `${email}.crt`,
                            extra: {
                                ...this.file.extra,
                                certificate,
                                pem,
                                ownerName: name,
                                ownerEmail: email
                            }
                        };
                    }
                } catch {
                    this.adaptedFile = this.file;
                }
            },
            immediate: true
        }
    },
    destroyed() {
        URL.revokeObjectURL(this.url);
    },
    methods: {
        async certificateFromBase64(base64Part) {
            const base64 = base64ToArrayBuffer(base64Part.split(",").pop());

            let certificate, pem;
            if (this.file.mime === MimeType.PKCS_7_SIGNED_DATA) {
                const envelope = getSignedDataEnvelope(base64);
                certificate = envelope.certificates[0];
                pem = pki.certificateToPem(certificate);
            } else {
                pem = await new Blob([base64]).text();
                certificate = pki.certificateFromPem(pem);
            }
            return { certificate, pem };
        }
    },
    render() {
        return this.$scopedSlots.default({ file: this.adaptedFile });
    }
};

function extractCertificateInfos(certificate) {
    const email = certificate.subject.attributes.find(({ name }) => name === "emailAddress")?.value;
    const name = certificate.subject.attributes.find(({ name }) => name === "commonName")?.value;
    return { email, name };
}

function canAdapt(file) {
    const supportedMimeTypes = [
        MimeType.X509_CERT,
        MimeType.CRYPTO_CERT,
        MimeType.PEM_FILE,
        MimeType.PKCS_7_SIGNED_DATA
    ];
    return supportedMimeTypes.includes(file.mime) && file.key.includes(":"); // if ":" is in fileKey, 2nd part is messageKey
}
</script>
