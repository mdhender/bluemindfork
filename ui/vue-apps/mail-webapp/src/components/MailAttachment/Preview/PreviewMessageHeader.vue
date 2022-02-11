<template>
    <div class="preview-message-header p-3 d-flex align-items-center" @click="$emit('click')">
        <bm-icon :icon="icon" />
        <span class="font-weight-bold ml-2"> Message </span>
    </div>
</template>
<script>
import { BmIcon } from "@bluemind/styleguide";

export default {
    name: "PreviewMessageHeader",
    components: { BmIcon },
    props: {
        icon: {
            type: String,
            required: true
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";

.preview-message-header {
    &:hover {
        background-color: $light;
        cursor: pointer;
    }
}
</style>
