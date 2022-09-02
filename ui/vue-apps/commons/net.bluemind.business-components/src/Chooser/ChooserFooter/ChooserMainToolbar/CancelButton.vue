<template>
    <bm-button class="cancel-button" variant="text">
        {{ $t("common.cancel") }}
    </bm-button>
</template>

<script>
import { BmButton } from "@bluemind/styleguide";

export default {
    name: "CancelButton",
    components: { BmButton }
};
</script>
