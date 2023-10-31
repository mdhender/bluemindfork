<template>
    <div class="chooser-main-toolbar">
        <cancel-button @click.native="$emit('cancel')" />
        <insert-button :disabled="disabledInsert" @click.native="$emit('insert')" />
    </div>
</template>

<script>
import CancelButton from "./CancelButton";
import InsertButton from "./InsertButton";

export default {
    name: "ChooserMainToolbar",
    components: { InsertButton, CancelButton },
    props: {
        disabledInsert: {
            type: Boolean,
            default: false
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.chooser-main-toolbar {
    display: flex;
    gap: $sp-5 $sp-7;
}
</style>
