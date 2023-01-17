<template>
    <span>
        <default-alert :alert="alert" />
        <bm-read-more-button
            href="https://doc.bluemind.net/release/5.0/guide_de_l_administrateur/configurer_les_domaines/signatures_d_entreprise"
        />
    </span>
</template>

<script>
import { DefaultAlert } from "@bluemind/alert.store";
export default {
    name: "CorporateSignatureAlert",
    components: { DefaultAlert },
    props: {
        alert: {
            type: Object,
            required: true
        }
    }
};
</script>
