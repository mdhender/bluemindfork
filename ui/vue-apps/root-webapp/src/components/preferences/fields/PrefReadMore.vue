<template>
    <div class="pref-read-more">
        <div v-if="text">
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
    name: "PrefReadMore",
    components: { BmButton },
    props: {
        href: {
            type: String,
            required: true
        },
        text: {
            type: String,
            default: null
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.pref-read-more {
    color: $neutral-fg-lo1;
    display: flex;
    flex-wrap: wrap;
    gap: $sp-1 $sp-3;
}
</style>
