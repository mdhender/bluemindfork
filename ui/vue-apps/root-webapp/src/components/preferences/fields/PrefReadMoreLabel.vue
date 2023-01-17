<template>
    <div class="pref-read-more-label">
        <div class="regular">
            {{ label }}
        </div>
        <bm-read-more :href="href" />
    </div>
</template>

<script>
import { BmReadMore } from "@bluemind/ui-components";

export default {
    name: "PrefReadMoreLabel",
    components: { BmReadMore },
    props: {
        label: {
            type: String,
            required: true
        },
        href: {
            type: String,
            required: true
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

.pref-read-more-label {
    display: flex;
    flex-wrap: wrap;
    gap: $sp-2 $sp-4;
}
</style>
