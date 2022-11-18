<template>
    <bm-modal v-model="show" centered :title="$t('preferences.mail.security.smime.import_certificate_modal.title')">
        <bm-label-icon v-if="!SMIME_AVAILABLE" class="mb-3" icon="info-circle">
            {{ $t("preferences.mail.security.smime.import_certificate_modal.supported_formats") }}
        </bm-label-icon>
        <bm-label-icon v-if="invalidFile" class="text-danger mb-3" icon="exclamation-circle-fill">
            {{ $t("preferences.mail.security.smime.import_certificate_modal.unsupported_file_type") }}
        </bm-label-icon>
        <bm-file-drop-zone
            v-if="!SMIME_AVAILABLE"
            :should-activate-fn="shouldActivate"
            always-show-dropzone
            class="mt-4"
            @drop-files="dropFile($event)"
        >
            <template #dropZone>
                <div class="text-center my-6">
                    <h2 class="mt-4 mb-6">
                        {{ $t("preferences.mail.security.smime.import_certificate_modal.label") }}
                    </h2>
                    <div class="mb-4">{{ $t("common.or") }}</div>
                    <bm-button variant="fill-accent" @click="openFilePicker">{{ $t("common.browse") }}</bm-button>
                </div>
            </template>
        </bm-file-drop-zone>
        <bm-spinner v-else-if="uploadStatus === 'IN_PROGRESS'" />
        <div v-else-if="uploadStatus === 'ERROR'">
            <bm-icon icon="exclamation-circle" size="lg" class="text-danger" />
            {{ $t("common.import_error") }}
        </div>
        <div class="mt-6">
            <bm-label-icon v-if="hasPrivateKey" class="text-success" icon="check-circle">
                {{ $t("preferences.mail.security.smime.import_certificate_modal.private_key_associated") }}
            </bm-label-icon>
            <bm-label-icon v-else class="text-danger" icon="exclamation-circle">
                {{ $t("preferences.mail.security.smime.import_certificate_modal.private_key_disassociated") }}
            </bm-label-icon>
            <bm-label-icon v-if="hasPublicCert" class="text-success mt-4" icon="check-circle">
                {{ $t("preferences.mail.security.smime.import_certificate_modal.pub_cert_associated") }}
            </bm-label-icon>
            <bm-label-icon v-else class="text-danger mt-4" icon="exclamation-circle">
                {{ $t("preferences.mail.security.smime.import_certificate_modal.pub_cert_disassociated") }}
            </bm-label-icon>
        </div>
        <input
            ref="fileChooserRef"
            :accept="allowedFileTypes.join(',')"
            tabindex="-1"
            aria-hidden="true"
            type="file"
            hidden
            @change="dropFile($event.target.files)"
        />
    </bm-modal>
</template>

<script>
import { mapGetters, mapMutations, mapState } from "vuex";
import { MimeType } from "@bluemind/email";
import { BmButton, BmFileDropZone, BmIcon, BmLabelIcon, BmModal, BmSpinner } from "@bluemind/ui-components";
import { SMIME_AVAILABLE } from "../../store/getterTypes";
import { SET_HAS_PRIVATE_KEY, SET_HAS_PUBLIC_CERT } from "../../store/mutationTypes";
import { SMIME_INTERNAL_API_URL, PKIEntry } from "../../lib/constants";

export default {
    name: "ImportSmimeKeyModal",
    components: { BmButton, BmFileDropZone, BmIcon, BmModal, BmLabelIcon, BmSpinner },
    data() {
        return {
            show: false,
            allowedFileTypes: [MimeType.PKCS_8, MimeType.CRYPTO_CERT],
            uploadStatus: "IDLE",
            invalidFile: false
        };
    },
    computed: {
        ...mapState("smime", ["hasPrivateKey", "hasPublicCert"]),
        ...mapGetters("smime", [SMIME_AVAILABLE])
    },
    methods: {
        ...mapMutations("smime", { SET_HAS_PRIVATE_KEY, SET_HAS_PUBLIC_CERT }),
        open() {
            this.uploadStatus = "IDLE";
            this.show = true;
        },
        openFilePicker() {
            this.$refs.fileChooserRef.click();
        },
        dropFile(files) {
            const file = files[0];
            this.invalidFile = file && !this.allowedFileTypes.includes(file.type);
            if (!file || this.invalidFile) {
                return;
            }
            this.upload(file);
        },
        async upload(file) {
            this.uploadStatus = "IN_PROGRESS";
            const kind = file.type === MimeType.PKCS_8 ? PKIEntry.PRIVATE_KEY : PKIEntry.CERTIFICATE;
            try {
                const url = `${SMIME_INTERNAL_API_URL}/${kind}`;
                const options = {
                    method: "PUT",
                    headers: { "Content-Type": file.type },
                    body: file
                };
                await fetch(url, options);
                if (kind === PKIEntry.PRIVATE_KEY) {
                    this.SET_HAS_PRIVATE_KEY(true);
                } else {
                    this.SET_HAS_PUBLIC_CERT(true);
                }
                this.uploadStatus = "IDLE";
            } catch {
                this.uploadStatus = "ERROR";
            }
        },
        shouldActivate(event) {
            const files = event.dataTransfer.items.length
                ? Object.keys(event.dataTransfer.items).map(key => event.dataTransfer.items[key])
                : [];
            const matchFunction = file => this.allowedFileTypes.includes(file.type);
            return files.some(matchFunction);
        }
    }
};
</script>
