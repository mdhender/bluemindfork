<template>
    <span class="pref-read-more-label">
        {{ text }}
        <bm-button variant="link" :href="href" target="_blank">
            {{ $t("common.read_more") }}
        </bm-button>
    </span>
</template>
<script>
import { BmButton } from "@bluemind/styleguide";

export default {
    name: "PrefReadMoreLabel",
    components: { BmButton },
    props: {
        href: {
            type: String,
            required: true
        },
        text: {
            type: String,
            required: true
        }
    }
};
</script>
