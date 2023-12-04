<script setup>
import { BmSpinner } from "@bluemind/ui-components";
</script>
<template>
    <div class="pt-5 text-center">
        {{ $t("common.searching") }}
        <bm-spinner class="pt-3" />
    </div>
</template>
