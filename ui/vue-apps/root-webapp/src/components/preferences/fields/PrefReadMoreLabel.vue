<template>
    <div class="pref-read-more-label">
        <div class="regular">
            {{ text }}
        </div>
        <bm-button variant="link" :href="href" target="_blank">
            {{ $t("common.read_more") }}
        </bm-button>
    </div>
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

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.pref-read-more-label {
    color: $neutral-fg-lo1;
    display: flex;
    flex-wrap: wrap;
    gap: $sp-1 $sp-3;

    .btn-link {
        margin-bottom: $sp-3;
    }
}
</style>
