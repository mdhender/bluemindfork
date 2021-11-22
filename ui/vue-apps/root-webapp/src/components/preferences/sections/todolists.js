import Roles from "@bluemind/roles";
import { mapExtensions } from "@bluemind/extensions";

export default function (i18n) {
    const tasks = mapExtensions("webapp.banner", ["application"]).application?.find(
        ({ $id }) => $id === "net.bluemind.webmodules.todolist"
    );
    return {
        id: "tasks",
        name: tasks?.name,
        icon: tasks?.icon,
        priority: tasks?.priority,
        visible: { name: "RoleCondition", args: [Roles.HAS_CALENDAR] },
        categories: [myTodoLists(i18n), otherTodoLists(i18n)]
    };
}

function myTodoLists(i18n) {
    return {
        id: "my_todo_lists",
        name: i18n.t("common.my_todo_lists"),
        icon: "list",
        groups: [
            {
                name: i18n.t("common.my_todo_lists"),
                id: "group",
                fields: [{ id: "field", component: { name: "PrefManageMyTodoLists" } }]
            }
        ]
    };
}

function otherTodoLists(i18n) {
    return {
        id: "other_todo_lists",
        name: i18n.t("common.other_todo_lists"),
        icon: "list-shared",
        groups: [
            {
                id: "group",
                name: i18n.t("common.other_todo_lists"),
                fields: [
                    {
                        id: "field",
                        component: { name: "PrefManageOtherTodoLists" }
                    }
                ]
            }
        ]
    };
}
