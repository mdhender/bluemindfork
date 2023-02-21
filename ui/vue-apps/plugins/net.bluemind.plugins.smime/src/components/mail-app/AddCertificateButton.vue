<template>
    <bm-icon-button
        v-if="!!pemCertificate"
        variant="compact"
        class="add-certificate-button"
        :size="iconSize"
        icon="verified-new"
        :title="$t('smime.mailapp.viewer.import_certificate.action', { email: file.extra.ownerEmail })"
        @click.stop="addCertificate"
    />
</template>

<script>
import { BmIconButton } from "@bluemind/ui-components";
import addCertificate from "../../lib/addCertificate";

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
        addCertificate() {
            addCertificate(this.pemCertificate, this.file.extra.ownerName, this.file.extra.ownerEmail);
        }
    }
};
</script>
