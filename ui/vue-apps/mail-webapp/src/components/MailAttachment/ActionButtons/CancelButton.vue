<template>
    <bm-button-close class="cancel-button" size="sm" :title="$tc('common.cancel')" @click.stop="$emit('cancel')" />
</template>

<script>
import { BmButtonClose } from "@bluemind/ui-components";

export default {
    name: "CancelButton",
    components: { BmButtonClose }
};
</script>
