<template>
    <div class="chooser-main-toolbar">
        <insert-button :disabled="disabledInsert" @click.native="$emit('insert')" />
        <cancel-button @click.native="$emit('cancel')" />
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
@import "~@bluemind/styleguide/css/_variables";

.chooser-main-toolbar {
    display: flex;
    gap: $sp-5 $sp-6;
}
</style>
