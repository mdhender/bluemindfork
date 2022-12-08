<template>
    <div class="pt-5 text-center">
        {{ $t("common.searching") }}
        <bm-spinner class="pt-3" />
    </div>
</template>

<script>
import { BmSpinner } from "@bluemind/ui-components";
export default {
    name: "SearchResultSpinner",
    components: {
        BmSpinner
    }
};
</script>

<style></style>
