<template>
    <h1 class="d-inline">
        <bm-badge pill :closeable="closeable" class="mr-2" @close="$emit('close')">
            {{ todoList.name }}
        </bm-badge>
    </h1>
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
