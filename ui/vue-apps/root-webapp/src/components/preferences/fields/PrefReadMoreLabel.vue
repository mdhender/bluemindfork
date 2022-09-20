<template>
    <div class="pref-read-more-label">
        <div class="regular">
            {{ label }}
        </div>
        <pref-read-more :href="href" />
    </div>
</template>

<script>
import PrefReadMore from "./PrefReadMore";

export default {
    name: "PrefReadMoreLabel",
    components: { PrefReadMore },
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
@import "~@bluemind/styleguide/css/_variables";

.pref-read-more-label {
    display: flex;
    flex-wrap: wrap;
    gap: $sp-2 $sp-4;
}
</style>
