<template>
    <div class="text-truncate"><bm-icon icon="list" class="mr-4" /> {{ todoList.name }}</div>
</template>

<script>
import { BmIcon } from "@bluemind/styleguide";

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
