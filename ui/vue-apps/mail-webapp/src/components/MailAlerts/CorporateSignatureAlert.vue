<template>
    <span>
        <default-alert :alert="alert" />
        <a
            class="text-nowrap"
            target="_blank"
            href="https://doc.bluemind.net/release/5.0/guide_de_l_administrateur/configurer_les_domaines/signatures_d_entreprise"
        >
            {{ $t("common.read_more") }}
        </a>
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
