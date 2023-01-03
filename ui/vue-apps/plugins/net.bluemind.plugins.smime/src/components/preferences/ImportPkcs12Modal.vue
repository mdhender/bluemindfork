<template>
    <bm-modal
        v-model="show"
        centered
        :title="$t('smime.preferences.import_field.modal.title')"
        :ok-disabled="!file || !!importError"
        @ok.prevent="importPkcs12"
    >
        <bm-label-icon v-if="!file" class="mb-3" icon="info-circle">
            {{ $t("smime.preferences.import_field.modal.supported_formats") }}
        </bm-label-icon>
        <bm-label-icon v-if="invalidFile" class="text-danger mb-3" icon="exclamation-circle-fill">
            {{ $t("smime.preferences.import_field.modal.unsupported_file") }}
        </bm-label-icon>
        <bm-form class="mt-4">
            <template v-if="file">
                <bm-label-icon class="text-success" icon="check-circle">{{ file.name }}</bm-label-icon>
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
                    <div class="text-center my-6">
                        <h2 class="mt-4 mb-6">{{ $t("common.drop_file") }}</h2>
                        <div class="mb-4">{{ $t("common.or") }}</div>
                        <bm-button variant="fill-accent" @click="openFilePicker">{{ $t("common.browse") }}</bm-button>
                    </div>
                </template>
            </bm-file-drop-zone>
            <bm-label-icon v-if="importError" class="text-danger" icon="exclamation-circle">
                {{ importError }}
            </bm-label-icon>
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
import {
    BmButton,
    BmFileDropZone,
    BmForm,
    BmFormGroup,
    BmFormInput,
    BmLabelIcon,
    BmModal
} from "@bluemind/ui-components";
import { SET_HAS_PRIVATE_KEY, SET_HAS_PUBLIC_CERT } from "../../store/mutationTypes";
import { SMIME_INTERNAL_API_URL, PKIEntry } from "../../lib/constants";

export default {
    name: "ImportPkcs12Modal",
    components: { BmButton, BmFileDropZone, BmForm, BmFormGroup, BmFormInput, BmModal, BmLabelIcon },
    data() {
        return {
            show: false,

            accept: [MimeType.PKCS_12, ...MimeType.PKCS_12_SUFFIXES.map(suffix => "." + suffix)].join(","),
            allowedFileType: MimeType.PKCS_12,
            importError: "",
            invalidFile: false,
            file: undefined,

            password: "",
            showPassword: false,
            isPasswordValid: true
        };
    },
    methods: {
        ...mapMutations("mail", { SET_HAS_PRIVATE_KEY, SET_HAS_PUBLIC_CERT }),
        open() {
            this.file = undefined;
            this.invalidFile = false;
            this.importError = "";
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
            this.invalidFile = file && file.type !== this.allowedFileType;
            if (file && !this.invalidFile) {
                this.file = file;
            }
        },
        async importPkcs12() {
            const p12Der = await this.file.arrayBuffer();
            const p12Asn1 = asn1.fromDer(new util.ByteStringBuffer(p12Der)); // p12 as ASN.1 object
            const p12 = this.decryptPkcs12(p12Asn1);

            const pemCert = this.extractCert(p12);
            const pemPrivateKey = this.extractPrivateKey(p12);
            if (pemCert && pemPrivateKey) {
                await this.upload(pemCert, pemPrivateKey);
                this.$emit("ok");
                this.show = false;
            }
        },
        decryptPkcs12(p12Asn1) {
            let p12;
            try {
                p12 = pkcs12.pkcs12FromAsn1(p12Asn1, this.password); // decrypt p12
            } catch (error) {
                console.error(error);
                if (error.message.includes("Invalid password")) {
                    this.isPasswordValid = false;
                } else {
                    this.importError = error.message;
                }
                return;
            }
            this.isPasswordValid = true;
            return p12;
        },
        extractCert(p12) {
            const certBags = p12.getBags({ bagType: pki.oids.certBag });
            if (certBags[pki.oids.certBag].length === 0) {
                this.importError = this.$t("smime.preferences.import_field.modal.no_cert_found");
                return;
            }
            const cert = certBags[pki.oids.certBag][0].cert;
            return pki.certificateToPem(cert);
        },
        extractPrivateKey(p12) {
            const keyBags = p12.getBags({ bagType: pki.oids.pkcs8ShroudedKeyBag });
            if (keyBags[pki.oids.pkcs8ShroudedKeyBag].length === 0) {
                this.importError = this.$t("smime.preferences.import_field.modal.no_private_key_found");
                return;
            }
            const privateKey = keyBags[pki.oids.pkcs8ShroudedKeyBag][0].key;
            return pki.privateKeyToPem(privateKey);
        },
        async upload(pemCert, pemPrivateKey) {
            let url = `${SMIME_INTERNAL_API_URL}/${PKIEntry.CERTIFICATE}`;
            const options = {
                method: "PUT",
                headers: { "Content-Type": MimeType.PEM_FILE },
                body: pemCert
            };
            await fetch(url, options);
            this.SET_HAS_PUBLIC_CERT(true);

            url = `${SMIME_INTERNAL_API_URL}/${PKIEntry.PRIVATE_KEY}`;
            options.body = pemPrivateKey;
            await fetch(url, options);
            this.SET_HAS_PRIVATE_KEY(true);
        },
        shouldActivate(event) {
            const files = event.dataTransfer.items;
            if (files.length > 0) {
                return files[0].type === this.allowedFileType;
            }
            return false;
        }
    }
};
</script>
