import Roles from "@bluemind/roles";
import { mapExtensions } from "@bluemind/extensions";

export default function (i18n) {
    const contact = mapExtensions("webapp.banner", ["application"]).application?.find(
        ({ $id }) => $id === "net.bluemind.webmodules.contact"
    );
    return {
        id: "contacts",
        name: i18n.t("common.application.contacts"),
        icon: contact?.icon,
        priority: contact?.priority,
        visible: { name: "RoleCondition", args: [Roles.HAS_MAIL] },
        categories: [myAddressBooks(i18n), otherAddressBooks(i18n)]
    };
}

function myAddressBooks(i18n) {
    return {
        id: "my_address_books",
        name: i18n.t("common.my_address_books"),
        icon: "addressbook",
        groups: [
            {
                name: i18n.t("common.my_address_books"),
                id: "group",
                fields: [{ id: "field", component: { name: "PrefManageMyAddressBooks" } }]
            }
        ]
    };
}

function otherAddressBooks(i18n) {
    return {
        id: "other_address_books",
        name: i18n.t("common.other_address_books"),
        icon: "addressbook-shared",
        groups: [
            {
                id: "group",
                name: i18n.t("common.other_address_books"),
                fields: [
                    {
                        id: "field",
                        component: { name: "PrefManageOtherAddressBooks" }
                    }
                ]
            }
        ]
    };
}
