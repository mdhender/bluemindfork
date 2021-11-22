import { Verb } from "@bluemind/core.container.api";

export const TodoListAcl = {
    HAS_NO_RIGHTS: 0,
    CAN_READ_MY_TODO_LIST: 1,
    CAN_EDIT_MY_TODO_LIST: 2,
    CAN_MANAGE_SHARES: 3
};

export function todoListAclToVerb(acl) {
    switch (acl) {
        case TodoListAcl.CAN_READ_MY_TODO_LIST:
            return Verb.Read;
        case TodoListAcl.CAN_EDIT_MY_TODO_LIST:
            return Verb.Write;
        case TodoListAcl.CAN_MANAGE_SHARES:
            return Verb.All;
        default:
            throw "impossible case : no acl equivalent";
    }
}

export function verbToTodoListAcl(verb) {
    switch (verb) {
        case Verb.Read:
            return TodoListAcl.CAN_READ_MY_TODO_LIST;
        case Verb.Write:
            return TodoListAcl.CAN_EDIT_MY_TODO_LIST;
        case Verb.Manage:
        case Verb.All:
            return TodoListAcl.CAN_MANAGE_SHARES;
        default:
            throw "impossible case : no acl equivalent";
    }
}

export function getTodoListOptions(i18n, count) {
    return [
        {
            text: i18n.tc("preferences.manage_shares.has_no_rights", count),
            value: TodoListAcl.HAS_NO_RIGHTS
        },
        {
            text: i18n.tc("preferences.tasks.can_read_my_todolist", count),
            value: TodoListAcl.CAN_READ_MY_TODO_LIST
        },
        {
            text: i18n.tc("preferences.tasks.can_edit_my_todolist", count),
            value: TodoListAcl.CAN_EDIT_MY_TODO_LIST
        },
        {
            text: i18n.tc("preferences.tasks.can_edit_my_todolist_and_manage_shares", count),
            value: TodoListAcl.CAN_MANAGE_SHARES
        }
    ];
}

export function defaultTodoListDirEntryAcl() {
    return TodoListAcl.CAN_READ_MY_TODO_LIST;
}

export function defaultTodoListDomainAcl() {
    return TodoListAcl.HAS_NO_RIGHTS;
}
