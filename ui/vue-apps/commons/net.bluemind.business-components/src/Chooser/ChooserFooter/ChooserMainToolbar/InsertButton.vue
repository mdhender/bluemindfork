<template>
    <bm-button class="insert-button" variant="fill-accent" :disabled="disabled">
        {{ $t("common.insert") }}
    </bm-button>
</template>

<script>
import { BmButton } from "@bluemind/ui-components";

export default {
    name: "InsertButton",
    components: { BmButton },
    props: {
        disabled: {
            type: Boolean,
            default: false
        }
    }
};
</script>
