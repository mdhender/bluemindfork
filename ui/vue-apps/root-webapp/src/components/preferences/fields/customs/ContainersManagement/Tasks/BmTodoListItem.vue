<template>
    <div class="d-flex align-items-center">
        <bm-icon icon="list" class="mr-4" />
        <div class="text-truncate">{{ todoList.name }}</div>
    </div>
</template>

<script>
import { BmIcon } from "@bluemind/ui-components";

export default {
    name: "BmTodoListItem",
    components: { BmIcon },
    props: {
        todoList: {
            type: Object,
            required: true
        }
    }
};
</script>
