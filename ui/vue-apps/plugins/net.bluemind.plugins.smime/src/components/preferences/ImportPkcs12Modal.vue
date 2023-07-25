<template>
    <bm-modal
        v-model="show"
        size="sm"
        centered
        content-class="import-pkcs12-modal-content"
        :title="$t('smime.preferences.import_field.modal.title')"
        :cancel-title="$t('common.cancel')"
        :ok-disabled="!file || !!importErrorMsg"
        @ok.prevent="importPkcs12"
    >
        <bm-form>
            <template v-if="file">
                <bm-label-icon v-if="importErrorMsg" class="text-error" icon="exclamation-circle-fill">{{
                    file.name
                }}</bm-label-icon>
                <bm-label-icon v-else class="text-success" icon="check-circle">{{ file.name }}</bm-label-icon>
                <bm-form-group
                    class="mt-6"
                    label-for="password"
                    :label="$t('smime.preferences.import_field.modal.unlock_pkcs12')"
                    :invalid-feedback="$t('common.invalid_password')"
                    :state="isPasswordValid"
                >
                    <bm-form-input
                        id="password"
                        v-model="password"
                        :type="showPassword ? 'text' : 'password'"
                        autofocus
                        required
                        actionable-icon
                        icon="eye"
                        @keydown.enter.prevent="importPkcs12"
                        @icon-click="showPassword = !showPassword"
                    />
                </bm-form-group>
            </template>
            <bm-file-drop-zone
                v-else
                :should-activate-fn="shouldActivate"
                always-show-dropzone
                @drop-files="dropFile($event)"
            >
                <template #dropZone>
                    <div class="drop-zone-content">
                        <div class="icon-and-text">
                            <bm-icon icon="file-type-certificate" size="lg" />
                            {{ $t("smime.preferences.import_field.modal.drop_file") }}
                        </div>
                        <div class="mb-5">{{ $t("common.or") }}</div>
                        <bm-button variant="fill-accent" @click="openFilePicker">{{ $t("common.browse") }}</bm-button>
                    </div>
                </template>
            </bm-file-drop-zone>
            <bm-label-icon v-if="importErrorMsg" class="text-danger" icon="exclamation-circle">
                {{ importErrorMsg }}
            </bm-label-icon>
            <bm-read-more v-if="readMoreLink" :href="readMoreLink" class="pl-6" />
            <input
                ref="fileChooserRef"
                :accept="accept"
                tabindex="-1"
                aria-hidden="true"
                type="file"
                hidden
                @change="dropFile($event.target.files)"
            />
        </bm-form>
    </bm-modal>
</template>

<script>
import { asn1, pkcs12, pki, util } from "node-forge";
import { mapMutations } from "vuex";
import { MimeType } from "@bluemind/email";
import { inject } from "@bluemind/inject";
import {
    BmButton,
    BmFileDropZone,
    BmForm,
    BmFormGroup,
    BmFormInput,
    BmIcon,
    BmLabelIcon,
    BmModal,
    BmReadMore
} from "@bluemind/ui-components";
import { SET_HAS_PRIVATE_KEY, SET_HAS_PUBLIC_CERT } from "../../store/root-app/types";
import { SMIME_INTERNAL_API_URL, PKIEntry, smimeErrorMsgRegex, CRYPTO_HEADERS } from "../../lib/constants";
import DocLinkMixin from "../../mixins/DocLinkMixin";
import { InvalidCertificateError, InvalidKeyError } from "../../lib/exceptions";

export default {
    name: "ImportPkcs12Modal",
    components: {
        BmButton,
        BmFileDropZone,
        BmForm,
        BmFormGroup,
        BmFormInput,
        BmIcon,
        BmLabelIcon,
        BmModal,
        BmReadMore
    },
    mixins: [DocLinkMixin],
    data() {
        return {
            show: false,

            accept: [
                MimeType.PKCS_12,
                MimeType.X_PKCS_12,
                ...MimeType.PKCS_12_SUFFIXES.map(suffix => "." + suffix)
            ].join(","),
            allowedFileTypes: [MimeType.PKCS_12, MimeType.X_PKCS_12],
            importErrorMsg: "",
            readMoreLink: "",
            unsupportedFile: false,
            file: undefined,

            password: "",
            showPassword: false,
            isPasswordValid: true
        };
    },
    methods: {
        ...mapMutations("smime", { SET_HAS_PRIVATE_KEY, SET_HAS_PUBLIC_CERT }),
        open() {
            this.file = undefined;
            this.unsupportedFile = false;
            this.importErrorMsg = "";
            this.readMoreLink = "";
            this.isPasswordValid = true;
            this.password = "";
            this.showPassword = false;
            this.show = true;
        },
        openFilePicker() {
            this.$refs.fileChooserRef.click();
        },
        dropFile(files) {
            const file = files[0];
            this.unsupportedFile = file && !this.allowedFileTypes.includes(file.type);
            if (file && !this.unsupportedFile) {
                this.file = file;
            }
        },
        async importPkcs12() {
            const p12Der = await this.file.arrayBuffer();
            const p12Asn1 = asn1.fromDer(new util.ByteStringBuffer(p12Der)); // p12 as ASN.1 object
            const p12 = this.decryptPkcs12(p12Asn1);

            const pemCert = this.extractCert(p12);
            const pemPrivateKey = this.extractPrivateKey(p12);
            await this.upload(pemCert, pemPrivateKey);
        },
        decryptPkcs12(p12Asn1) {
            let p12;
            try {
                p12 = pkcs12.pkcs12FromAsn1(p12Asn1, this.password); // decrypt p12
            } catch (error) {
                if (error.message.includes("Invalid password")) {
                    this.isPasswordValid = false;
                } else {
                    this.importErrorMsg = error.message;
                }
                throw error;
            }
            this.isPasswordValid = true;
            return p12;
        },
        extractCert(p12) {
            const certBags = p12.getBags({ bagType: pki.oids.certBag });
            if (certBags[pki.oids.certBag].length === 0) {
                this.importErrorMsg = this.$t("smime.preferences.import_field.modal.no_cert_found");
                throw "no cert found in PKCS12";
            }
            const cert = certBags[pki.oids.certBag][0].cert;
            try {
                return pki.certificateToPem(cert);
            } catch {
                const error = new InvalidCertificateError();
                this.importErrorMsg = error.message;
                this.readMoreLink = this.linkFromCode(error.code);
                throw error;
            }
        },
        extractPrivateKey(p12) {
            const keyBags = p12.getBags({ bagType: pki.oids.pkcs8ShroudedKeyBag });
            if (keyBags[pki.oids.pkcs8ShroudedKeyBag].length === 0) {
                this.importErrorMsg = this.$t("smime.preferences.import_field.modal.no_private_key_found");
                throw "no key found in PKCS12";
            }
            const privateKey = keyBags[pki.oids.pkcs8ShroudedKeyBag][0].key;
            try {
                return pki.privateKeyToPem(privateKey);
            } catch {
                const error = new InvalidKeyError();
                this.importErrorMsg = error.message;
                this.readMoreLink = this.linkFromCode(error.code);
                throw error;
            }
        },
        async upload(pemCert, pemPrivateKey) {
            await this.uploadCert(pemCert);
            await this.uploadPrivateKey(pemPrivateKey);
            this.$emit("ok");
            this.show = false;
        },
        async uploadCert(pemCert) {
            const email = inject("UserSession").defaultEmail;
            const url = `${SMIME_INTERNAL_API_URL}/${PKIEntry.CERTIFICATE}?email=${email}`;
            const response = await fetch(url, {
                method: "PUT",
                headers: { "Content-Type": MimeType.PEM_FILE },
                body: pemCert
            });
            if (!response.ok) {
                const errorMsg = await response.text();
                if (smimeErrorMsgRegex.test(errorMsg)) {
                    const matches = errorMsg.match(smimeErrorMsgRegex);
                    const code = parseInt(matches[2]);
                    this.importErrorMsg =
                        code & CRYPTO_HEADERS.UNTRUSTED_CERTIFICATE_EMAIL_NOT_FOUND
                            ? this.$t("smime.preferences.import_field.modal.email_not_found", { email })
                            : errorMsg.replace(matches[0], "");
                    this.readMoreLink = this.linkFromCode(code);
                } else {
                    this.importErrorMsg = errorMsg;
                }
                throw "uploading cert failed";
            }
            this.SET_HAS_PUBLIC_CERT(true);
        },
        async uploadPrivateKey(pemPrivateKey) {
            const url = `${SMIME_INTERNAL_API_URL}/${PKIEntry.PRIVATE_KEY}`;
            const options = {
                method: "PUT",
                headers: { "Content-Type": MimeType.PEM_FILE },
                body: pemPrivateKey
            };
            await fetch(url, options);
            this.SET_HAS_PRIVATE_KEY(true);
        },
        shouldActivate(event) {
            const files = event.dataTransfer.items;
            if (files.length > 0) {
                return this.allowedFileTypes.includes(files[0].type);
            }
            return false;
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.import-pkcs12-modal-content {
    form {
        height: base-px-to-rem(160);
        display: flex;
        flex-direction: column;
        margin-top: $sp-2;
        gap: $sp-5;

        .bm-file-drop-zone {
            height: 100%;

            .drop-zone-content {
                flex: 1;

                &,
                .icon-and-text {
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    text-align: center;
                }

                justify-content: space-evenly;

                .icon-and-text {
                    gap: $sp-3;
                }
            }
        }
    }
}
</style>
