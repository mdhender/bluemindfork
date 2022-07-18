<template>
    <bm-button-close
        variant="inline-neutral"
        class="remove-button p-0"
        size="md"
        :title="$tc('common.removeAttachment')"
        @click.stop="$emit('remove')"
    />
</template>

<script>
import { BmButtonClose } from "@bluemind/styleguide";

export default {
    name: "RemoveButton",
    components: { BmButtonClose }
};
</script>

<style></style>
