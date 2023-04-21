<template>
    <bm-icon-button
        v-if="!!pemCertificate"
        variant="compact"
        class="add-certificate-button"
        :size="iconSize"
        icon="verified-new"
        :title="$t('smime.mailapp.viewer.add_certificate.action', { email: file.extra.ownerEmail })"
        @click.stop="addCertificate"
    />
</template>

<script>
import { mapActions } from "vuex";
import { BmIconButton } from "@bluemind/ui-components";
import { ADD_CERTIFICATE } from "../../store/mail/types";

export default {
    name: "AddCertificateButton",
    components: { BmIconButton },
    props: {
        file: {
            type: Object,
            required: true
        },
        iconSize: {
            type: String,
            default: "sm"
        }
    },
    computed: {
        pemCertificate() {
            return this.file.extra.pem;
        }
    },
    methods: {
        ...mapActions("mail", { ADD_CERTIFICATE }),
        addCertificate() {
            this.ADD_CERTIFICATE({
                pem: this.pemCertificate,
                dn: this.file.extra.ownerName,
                email: this.file.extra.ownerEmail
            });
        }
    }
};
</script>
