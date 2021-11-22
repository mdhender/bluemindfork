<template>
    <containers-management
        :containers="otherTodoLists"
        :container-type="containerType"
        share-column
        @offline-sync-changed="UPDATE_OTHER_TODO_LIST"
        @remove="REMOVE_OTHER_TODO_LIST"
        @subscribe="ADD_OTHER_TODO_LIST"
        @update="UPDATE_OTHER_TODO_LIST"
    >
        <template v-slot:item="{ container }"><bm-todo-list-item :todo-list="container" /></template>
        <template v-slot:badge-item="{ container, closeFn }">
            <bm-todo-list-badge :todo-list="container" closeable @close="closeFn(container)" />
        </template>
    </containers-management>
</template>

<script>
import BmTodoListBadge from "./BmTodoListBadge";
import BmTodoListItem from "./BmTodoListItem";
import { ContainerType } from "../container";
import ContainersManagement from "../ContainersManagement";
import { mapMutations, mapState } from "vuex";

export default {
    name: "PrefManageOtherTodoLists",
    components: { BmTodoListBadge, BmTodoListItem, ContainersManagement },
    data() {
        return { containerType: ContainerType.TODOLIST };
    },
    computed: {
        ...mapState("preferences", ["otherTodoLists"])
    },
    methods: {
        ...mapMutations("preferences", ["ADD_OTHER_TODO_LIST", "REMOVE_OTHER_TODO_LIST", "UPDATE_OTHER_TODO_LIST"])
    }
};
</script>
