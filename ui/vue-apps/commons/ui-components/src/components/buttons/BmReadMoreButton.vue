<template>
    <bm-button class="bm-read-more-button" variant="link" :href="href"> {{ $t("common.read_more") }}</bm-button>
</template>

<script>
import BmButton from "./BmButton";

export default {
    name: "BmReadMoreButton",
    components: { BmButton },
    props: {
        href: {
            type: String,
            required: true
        }
    }
};
</script>

<style lang="scss">
@import "../../css/_variables";

.bm-read-more-button {
    padding: $sp-2;
}
</style>
