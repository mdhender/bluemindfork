<template>
    <bm-button-close
        variant="light"
        class="cancel-button p-0"
        size="md"
        :title="$tc('common.cancel')"
        @click.stop="$emit('cancel')"
    />
</template>

<script>
import { BmButtonClose } from "@bluemind/styleguide";

export default {
    name: "CancelButton",
    components: { BmButtonClose }
};
</script>
