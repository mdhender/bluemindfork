import { AvailableTimeFormats, AvailableDateFormats } from "@bluemind/date";
import { AvailableLanguages } from "@bluemind/i18n";
import Roles from "@bluemind/roles";
import NotificationManager from "../../../NotificationManager";

import themeSystem from "../../../../assets/theme-system.png";
import themeLight from "../../../../assets/theme-light.png";
import themeDark from "../../../../assets/theme-dark.png";

export default function (i18n) {
    return {
        id: "my_account",
        name: i18n.t("common.my_account"),
        icon: { name: "preferences" },
        priority: Number.MAX_SAFE_INTEGER,
        categories: [main(i18n), security(i18n), cti(i18n), delegates(i18n), advanced(i18n), externalAccounts(i18n)]
    };
}

function main(i18n) {
    const availableDefaultApps = [
        { text: i18n.t("common.application.webmail"), value: "/webapp/mail/" },
        { text: i18n.t("common.application.calendar"), value: "/cal/" }
    ];
    return {
        id: "main",
        name: i18n.t("common.general"),
        icon: "wrench",
        groups: [
            {
                id: "localisation",
                name: i18n.t("common.localisation"),
                fields: [
                    {
                        id: "lang",
                        component: {
                            name: "PrefFieldSelect",
                            options: {
                                choices: AvailableLanguages,
                                label: i18n.t("preferences.general.lang"),
                                setting: "lang",
                                needReload: true
                            }
                        }
                    },

                    {
                        id: "dateformat",
                        component: {
                            name: "PrefFieldSelect",
                            options: {
                                choices: AvailableDateFormats,
                                label: i18n.t("preferences.general.date_format"),
                                setting: "date",
                                autosave: true
                            }
                        }
                    },
                    {
                        id: "timeformat",
                        component: {
                            name: "PrefFieldSelect",
                            options: {
                                choices: AvailableTimeFormats,
                                label: i18n.t("preferences.general.time_format"),
                                setting: "timeformat",
                                autosave: true
                            }
                        }
                    }
                ]
            },
            {
                id: "default_app",
                name: i18n.t("preferences.general.default_application"),
                fields: [
                    {
                        id: "field",
                        component: {
                            name: "PrefFieldSelect",
                            options: {
                                choices: availableDefaultApps,
                                setting: "default_app",
                                needLogout: true
                            }
                        }
                    }
                ]
            },
            {
                id: "theme",
                name: i18n.t("preferences.general.theme"),
                fields: [
                    {
                        id: "field",
                        component: {
                            name: "PrefFieldChoice",
                            options: {
                                setting: "theme",
                                autosave: true,
                                choices: [
                                    {
                                        name: i18n.t("preferences.general.theme.system"),
                                        value: "system",
                                        img: themeSystem
                                    },
                                    {
                                        name: i18n.t("preferences.general.theme.light"),
                                        value: "light",
                                        img: themeLight
                                    },
                                    {
                                        name: i18n.t("preferences.general.theme.dark"),
                                        value: "dark",
                                        img: themeDark
                                    }
                                ],
                                default: "system"
                            }
                        }
                    }
                ]
            },
            {
                id: "tags",
                name: i18n.t("preferences.general.tags"),
                fields: [
                    {
                        id: "field",
                        component: {
                            name: "PrefTags",
                            options: { autosave: true }
                        }
                    }
                ]
            }
        ]
    };
}

function advanced(i18n) {
    return {
        id: "advanced",
        name: i18n.t("common.advanced"),
        icon: "plus",
        priority: -1,
        groups: [
            {
                id: "local_data",
                name: i18n.t("preferences.advanced.reinit_local_data"),
                fields: [
                    {
                        id: "field",
                        component: {
                            name: "PrefResetLocalData",
                            options: {
                                text: i18n.t("common.action.reset"),
                                label: i18n.t("preferences.advanced.reinit_local_data.explanations")
                            }
                        }
                    }
                ]
            },
            {
                id: "notification",
                name: i18n.t("preferences.advanced.notifications"),
                visible: new NotificationManager().isAvailable,
                fields: [
                    {
                        id: "field",
                        component: { name: "PrefEnableNotifications" }
                    }
                ]
            }
        ]
    };
}

function security(i18n) {
    return {
        id: "security",
        name: i18n.t("common.security"),
        icon: "key",
        groups: [
            {
                id: "password",
                name: i18n.t("common.password"),
                disabled: {
                    name: "RoleCondition.none",
                    args: [Roles.SELF_CHANGE_PASSWORD]
                },

                fields: [
                    {
                        id: "field",
                        component: { name: "PrefPassword" }
                    }
                ]
            },
            {
                id: "api_keys",
                name: i18n.t("preferences.security.api_key"),
                fields: [
                    {
                        id: "field",
                        component: { name: "PrefAPIKey" }
                    }
                ]
            }
        ]
    };
}

function cti(i18n) {
    return {
        id: "cti",
        name: i18n.t("common.telephony"),
        icon: "phone",
        visible: { name: "RoleCondition.every", args: [Roles.HAS_CTI] },
        groups: [
            {
                id: "status",
                name: i18n.t("preferences.telephony.status"),
                fields: [
                    {
                        id: "field",
                        component: {
                            name: "PrefIMSetPhonePresence",
                            options: {
                                setting: "im_set_phone_presence"
                            }
                        }
                    }
                ]
            }
        ]
    };
}

function externalAccounts(i18n) {
    return {
        id: "external_accounts",
        name: i18n.t("preferences.account.external_accounts"),
        icon: "user",
        visible: { name: "RoleCondition", args: [Roles.SELF_MANAGE_EXTERNAL_ACCOUNT] },
        groups: [
            {
                id: "creation",
                name: i18n.t("preferences.account.external_accounts.creation"),
                fields: [
                    {
                        id: "field",
                        component: { name: "PrefExtAccountCreation" }
                    }
                ]
            },
            {
                id: "list",
                name: i18n.t("preferences.account.external_accounts.list"),
                fields: [
                    {
                        id: "field",
                        component: { name: "PrefExtAccountList", options: { autosave: true } }
                    }
                ]
            }
        ]
    };
}

function delegates(i18n) {
    return {
        id: "delegates",
        name: i18n.t("preferences.account.delegates"),
        icon: "user-hierarchy",
        visible: { name: "RoleCondition", args: [Roles.HAS_MAIL] },
        groups: [
            {
                id: "group",
                name: i18n.t("preferences.account.delegates"),
                fields: [{ id: "field", component: { name: "PrefDelegates" } }]
            }
        ]
    };
}
