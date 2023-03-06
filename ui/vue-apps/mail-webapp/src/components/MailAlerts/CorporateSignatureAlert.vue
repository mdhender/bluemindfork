<template>
    <span>
        <default-alert :alert="alert" />
        <bm-read-more
            href="https://doc.bluemind.net/release/5.0/guide_de_l_administrateur/configurer_les_domaines/signatures_d_entreprise"
        />
    </span>
</template>

<script>
import { DefaultAlert } from "@bluemind/alert.store";
import { BmReadMore } from "@bluemind/ui-components";
export default {
    name: "CorporateSignatureAlert",
    components: { BmReadMore, DefaultAlert },
    props: {
        alert: {
            type: Object,
            required: true
        }
    }
};
</script>
