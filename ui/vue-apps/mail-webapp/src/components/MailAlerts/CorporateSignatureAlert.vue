<template>
    <span>
        <default-alert :alert="alert" />
        <a
            class="text-nowrap"
            target="_blank"
            href="https://forge.bluemind.net/confluence/display/BM4/Signatures+d%27entreprise"
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
