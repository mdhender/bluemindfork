<template>
    <div class="pt-5 font-size-lg">
        {{ $t("common.search.error") }} <br /><br />
        {{ $t("common.check.connection") }}
    </div>
</template>

<script>
export default {
    name: "SearchResultError"
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";
.font-size-lg {
    font-size: $font-size-lg;
}
</style>
