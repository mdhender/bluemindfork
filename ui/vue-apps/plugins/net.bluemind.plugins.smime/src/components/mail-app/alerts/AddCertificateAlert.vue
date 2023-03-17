<template>
    <div v-if="payload.ownerEmail && payload.pem" class="add-certificate-alert">
        <div>
            <i18n path="alert.smime.certificate">
                <template #name>
                    <span class="name">{{ payload.ownerName }}</span>
                </template>
            </i18n>
            <span class="email font-weight-light">&lt;{{ payload.ownerEmail }}&gt;</span>
        </div>
        <bm-label-icon
            v-if="done"
            class="feedback high"
            icon-size="sm"
            :icon="success ? 'check' : 'exclamation-circle'"
        >
            {{ $t(`alert.smime.add_certificate.${success ? "success" : "error"}`) }}
        </bm-label-icon>
        <bm-button v-else variant="text" size="sm" icon="verified-new" :loading="loading" @click="importCertificate">
            {{ $t("smime.mailapp.viewer.add_certificate.action_short") }}
        </bm-button>
    </div>
</template>

<script>
import { BmLabelIcon, BmButton } from "@bluemind/ui-components";
import addCertificate from "../../../lib/addCertificate";

export default {
    name: "AddCertificateAlert",
    components: { BmLabelIcon, BmButton },
    props: {
        alert: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            success: false,
            loading: false,
            error: false
        };
    },
    computed: {
        payload() {
            return this.alert.payload;
        },
        done() {
            return this.success || this.error;
        }
    },
    methods: {
        async importCertificate() {
            try {
                this.loading = true;
                await addCertificate(this.payload.pem, this.payload.ownerName, this.payload.ownerEmail);
                this.success = true;
                this.loading = false;
            } catch {
                this.error = true;
                this.loading = false;
            }
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
    .feedback {
        color: $neutral-fg;
    }
}
</style>
