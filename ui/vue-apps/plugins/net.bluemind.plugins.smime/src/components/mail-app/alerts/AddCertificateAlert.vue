<template>
    <div v-if="payload.ownerEmail && payload.pem" class="add-certificate-alert">
        <div>
            <i18n path="smime.mailapp.alert.certificate">
                <template #name>
                    <span class="name">{{ payload.ownerName }}</span>
                </template>
            </i18n>
            <span class="email font-weight-light">&lt;{{ payload.ownerEmail }}&gt;</span>
        </div>
        <bm-button variant="text" icon="verified-new" @click="importCertificate">
            {{ $t("smime.mailapp.viewer.import_certificate") }}
        </bm-button>
    </div>
</template>

<script>
import { BmButton } from "@bluemind/ui-components";
import addCertificate from "../../../lib/addCertificate";

export default {
    name: "AddCertificateAlert",
    components: { BmButton },
    props: {
        alert: {
            type: Object,
            required: true
        }
    },
    computed: {
        payload() {
            return this.alert.payload;
        }
    },
    methods: {
        importCertificate() {
            addCertificate(this.payload.pem, this.payload.ownerName, this.payload.ownerEmail);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

.add-certificate-alert {
    .name {
        font-weight: $font-weight-bold;
        color: $neutral-fg;
    }
    .email {
        font-weight: $font-weight-light;
        color: $neutral-fg;
    }
}
</style>
