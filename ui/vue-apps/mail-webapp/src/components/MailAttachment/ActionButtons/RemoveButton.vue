<template>
    <bm-button-close
        class="remove-button"
        size="sm"
        :title="$tc('common.removeAttachment')"
        @click.stop="$emit('remove')"
    />
</template>

<script>
import { BmButtonClose } from "@bluemind/ui-components";

export default {
    name: "RemoveButton",
    components: { BmButtonClose }
};
</script>
