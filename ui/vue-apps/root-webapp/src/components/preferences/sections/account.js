import { AvailableTimeFormats, AvailableDateFormats, AvailablesTimezones } from "@bluemind/date";
import { AvailableLanguages } from "@bluemind/i18n";
import Roles from "@bluemind/roles";
import NotificationManager from "../../../NotificationManager";

export default function (i18n) {
    return {
        id: "my_account",
        name: i18n.t("common.my_account"),
        icon: { name: "preferences" },
        priority: Number.MAX_SAFE_INTEGER,
        categories: [main(i18n), security(i18n), cti(i18n), advanced(i18n)]
    };
}

function main(i18n) {
    const availableDefaultApps = [
        { text: i18n.t("common.application.webmail"), value: "/webmail/" },
        { text: i18n.t("common.application.calendar"), value: "/cal/" }
    ];
    return {
        id: "my_account.main",
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
                        id: "timezone",
                        component: {
                            name: "PrefFieldComboBox",
                            options: {
                                choices: AvailablesTimezones,
                                label: i18n.t("preferences.general.timezone"),
                                setting: "timezone"
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
                                setting: "date"
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
                                setting: "timeformat"
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
                            options: { choices: availableDefaultApps, setting: "default_app", needLogout: true }
                        }
                    }
                ]
            },
            {
                id: "tags",
                name: i18n.t("preferences.general.tags"),
                fields: [{ id: "tags", component: { name: "PrefTags" } }]
            }
        ]
    };
}

function advanced(i18n) {
    return {
        id: "my_account.advanced",
        name: i18n.t("common.advanced"),
        icon: "plus",
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
        id: "my_account.security",
        name: i18n.t("common.security"),
        icon: "server",
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
        id: "my_account.cti",
        name: i18n.t("common.telephony"),
        icon: "cables",
        visible: { name: "RoleCondition.every", args: [Roles.HAS_CTI, Roles.HAS_IM] },
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
