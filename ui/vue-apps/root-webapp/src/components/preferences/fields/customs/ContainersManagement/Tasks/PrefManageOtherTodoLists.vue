<template>
    <containers-management
        :containers="otherTodoLists"
        :container-type="containerType"
        :collapsed="collapsed"
        :field-id="id"
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
import { ContainerHelper, ContainerType } from "../container";
import BaseField from "../../../../mixins/BaseField";
import ContainersManagement from "../ContainersManagement";
import TodoListHelper from "./helper";
import { mapMutations, mapState } from "vuex";

ContainerHelper.register(ContainerType.TODOLIST, TodoListHelper);

export default {
    name: "PrefManageOtherTodoLists",
    components: { BmTodoListBadge, BmTodoListItem, ContainersManagement },
    mixins: [BaseField],
    data() {
        return { containerType: ContainerType.TODOLIST };
    },
    computed: {
        ...mapState("preferences", { otherTodoLists: state => state.containers.otherTodoLists })
    },
    methods: {
        ...mapMutations("preferences", ["ADD_OTHER_TODO_LIST", "REMOVE_OTHER_TODO_LIST", "UPDATE_OTHER_TODO_LIST"])
    }
};
</script>
