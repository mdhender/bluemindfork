<template>
    <bm-badge pill :closeable="closeable" class="bm-todo-list-badge" @close="$emit('close')">
        {{ todoList.name }}
    </bm-badge>
</template>

<script>
import { BmBadge } from "@bluemind/styleguide";

export default {
    name: "BmTodoListBadge",
    components: { BmBadge },
    props: {
        todoList: {
            type: Object,
            required: true
        },
        closeable: {
            type: Boolean,
            default: false
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_type";
@import "~@bluemind/styleguide/css/_variables";

.badge.bm-todo-list-badge {
    padding: $sp-3 $sp-4 $sp-3 $sp-5;
    gap: $sp-4;
    @extend %caption-bold;
}
</style>
