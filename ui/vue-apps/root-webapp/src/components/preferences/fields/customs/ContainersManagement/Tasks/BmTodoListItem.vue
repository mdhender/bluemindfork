<template>
    <div><bm-icon icon="list" size="lg" /> {{ todoList.name }}</div>
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
