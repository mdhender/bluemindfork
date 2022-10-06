<template>
    <div v-if="isSWAvailable" class="pref-smime">
        <bm-spinner v-if="loading" />
        <div v-else-if="swError">
            {{ $t("preferences.mail.security.smime.service_worker_error") }}
        </div>
        <template v-else>
            <img :src="isAssociated ? setKeyIllustration : unsetKeyIllustration" class="mr-5" />
            <div class="d-inline-block align-middle">
                <template v-if="isAssociated">
                    <bm-label-icon icon="check-circle" icon-size="lg">
                        {{ $t("preferences.mail.security.smime.cert_and_key_associated.label") }}
                    </bm-label-icon>
                    <bm-button variant="text" class="d-block mt-5 ml-6" @click="dissociate">
                        {{ $t("preferences.mail.security.smime.certificate_disassociated.button") }}
                    </bm-button>
                </template>
                <template v-else>
                    {{ $t("preferences.mail.security.smime.certificate_disassociated.label") }}
                    <bm-button variant="text-accent" class="d-block mt-5 ml-4" @click="openUploadModal">
                        <bm-icon icon="key" class="mr-3" />
                        {{ $t("preferences.mail.security.smime.certificate_associated.button") }}
                    </bm-button>
                </template>
            </div>
        </template>
        <import-smime-key-modal ref="import-modal" @key-set="onKeySet" />
    </div>
    <div v-else>{{ $t("preferences.mail.security.smime.no_service_worker") }}</div>
</template>

<script>
import { BmButton, BmIcon, BmLabelIcon, BmSpinner } from "@bluemind/styleguide";
import ImportSmimeKeyModal from "./ImportSmimeKeyModal";
import unsetKeyIllustration from "../../assets/setting-encryption-key-unset.png";
import setKeyIllustration from "../../assets/setting-encryption-key-set.png";

// FIXME: with service-worker global env
const SW_INTERNAL_API_PATH = "/service-worker-internal/";

export default {
    name: "PrefSmime",
    components: { BmButton, BmIcon, BmLabelIcon, BmSpinner, ImportSmimeKeyModal },
    data() {
        return {
            isSWAvailable: navigator.serviceWorker?.controller,
            swError: false,
            loading: true,
            isPrivateKeyAssociated: false,
            isPublicCertificateAssociated: false,
            setKeyIllustration,
            unsetKeyIllustration
        };
    },
    computed: {
        isAssociated() {
            return this.isPublicCertificateAssociated && this.isPrivateKeyAssociated;
        }
    },
    mounted() {
        if (this.isSWAvailable) {
            this.hasPkcs12();
        }
    },
    methods: {
        async hasPkcs12() {
            this.loading = true;
            try {
                const url = new URL(SW_INTERNAL_API_PATH + "smime", self.location.origin);
                const options = { method: "GET" };
                const response = await fetch(url, options);
                const json = await response.json();
                this.isPrivateKeyAssociated = json.privateKey;
                this.isPublicCertificateAssociated = json.publicCert;
                this.swError = false;
            } catch (e) {
                this.swError = true;
            } finally {
                this.loading = false;
            }
        },
        onKeySet({ isSet, kind }) {
            if (isSet && kind === "privateKey") {
                this.isPrivateKeyAssociated = true;
            } else if (isSet && kind === "publicCert") {
                this.isPublicCertificateAssociated = true;
            }
        },
        openUploadModal() {
            this.$refs["import-modal"].open(this.isPrivateKeyAssociated, this.isPublicCertificateAssociated);
        },
        async dissociate() {
            this.loading = true;
            try {
                const url = new URL(SW_INTERNAL_API_PATH + "smime", self.location.origin);
                const options = { method: "DELETE" };
                await fetch(url, options);
                this.isPrivateKeyAssociated = false;
                this.isPublicCertificateAssociated = false;
                this.swError = false;
            } catch {
                this.swError = true;
            } finally {
                this.loading = false;
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.pref-smime {
    .fa-check-circle {
        color: $success-fg;
    }
}
</style>
