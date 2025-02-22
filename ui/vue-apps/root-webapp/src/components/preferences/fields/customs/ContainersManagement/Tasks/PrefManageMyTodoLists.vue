<template>
    <containers-management
        :containers="myTodoLists"
        :container-type="containerType"
        :create-container-fn="create"
        :field-id="id"
        manage-mine
        @offline-sync-changed="UPDATE_PERSONAL_TODO_LIST"
        @update="update"
        @remove="remove"
        @reset-data="resetData"
    >
        <template #item="{ container }"><bm-todo-list-item :todo-list="container" /></template>
    </containers-management>
</template>

<script>
import { mapActions, mapMutations, mapState } from "vuex";

import { inject } from "@bluemind/inject";

import { containerToModifiableDescriptor, ContainerHelper, ContainerType } from "../container";
import BmTodoListItem from "./BmTodoListItem";
import { BaseField } from "@bluemind/preferences";
import ContainersManagement from "../ContainersManagement";
import TodoListHelper from "./helper";

ContainerHelper.register(ContainerType.TODOLIST, TodoListHelper);

export default {
    name: "PrefManageMyTodoLists",
    components: { BmTodoListItem, ContainersManagement },
    mixins: [BaseField],
    data() {
        return { containerType: ContainerType.TODOLIST };
    },
    computed: {
        ...mapState("preferences", { myTodoLists: state => state.containers.myTodoLists })
    },
    methods: {
        ...mapActions("preferences", ["SUBSCRIBE_TO_CONTAINERS"]),
        ...mapMutations("preferences", [
            "ADD_PERSONAL_TODO_LIST",
            "REMOVE_PERSONAL_TODO_LIST",
            "UPDATE_PERSONAL_TODO_LIST"
        ]),
        resetData(container) {
            inject("TodoListPersistence", container.uid).reset();
        },
        async remove(container) {
            await inject("TodoListsPersistence").delete(container.uid);
            this.REMOVE_PERSONAL_TODO_LIST(container.uid);
        },
        async create(container) {
            await inject("TodoListsPersistence").create(container.uid, container, false);
            this.ADD_PERSONAL_TODO_LIST(container);
            this.SUBSCRIBE_TO_CONTAINERS([container]);
        },
        async update(container) {
            await inject("ContainersPersistence").update(container.uid, containerToModifiableDescriptor(container));
            this.UPDATE_PERSONAL_TODO_LIST(container);
        }
    }
};
</script>
