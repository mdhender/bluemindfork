import { AvailableTimeFormats, AvailableDateFormats, AvailablesTimezones } from "@bluemind/date";
import { AvailableLanguages } from "@bluemind/i18n";
import Roles from "@bluemind/roles";

import NotificationManager from "../../../NotificationManager";

export default function (roles, vueI18N) {
    const availableDefaultApps = [
        { text: vueI18N.t("common.application.webmail"), value: "/webmail/" },
        { text: vueI18N.t("common.application.calendar"), value: "/cal/" }
    ];

    const mainCategoryGroups = [
        {
            title: vueI18N.t("common.localisation"),
            fields: [
                {
                    name: vueI18N.t("preferences.general.lang"),
                    setting: "lang",
                    component: "PrefFieldSelect",
                    options: { choices: AvailableLanguages }
                },
                {
                    name: vueI18N.t("preferences.general.timezone"),
                    setting: "timezone",
                    component: "PrefFieldComboBox",
                    options: { choices: AvailablesTimezones }
                },
                {
                    name: vueI18N.t("preferences.general.date_format"),
                    setting: "date",
                    component: "PrefFieldSelect",
                    options: { choices: AvailableDateFormats }
                },
                {
                    name: vueI18N.t("preferences.general.time_format"),
                    setting: "timeformat",
                    component: "PrefFieldSelect",
                    options: { choices: AvailableTimeFormats }
                }
            ]
        },
        {
            title: vueI18N.t("preferences.general.default_application"),
            fields: [
                {
                    setting: "default_app",
                    component: "PrefFieldSelect",
                    options: { choices: availableDefaultApps }
                }
            ]
        }
    ];

    const advancedCategoryGroups = [
        {
            title: vueI18N.t("preferences.advanced.reinit_local_data"),
            fields: [
                {
                    component: "PrefResetLocalData",
                    options: {
                        text: vueI18N.t("common.action.reset"),
                        label: vueI18N.t("preferences.advanced.reinit_local_data.explanations")
                    }
                }
            ]
        },
        {
            title: vueI18N.t("preferences.advanced.notifications"),
            condition: new NotificationManager().isAvailable,
            fields: [
                {
                    component: "PrefEnableNotifications",
                    options: {}
                }
            ]
        }
    ];

    const securityCategoryGroups = [
        {
            title: vueI18N.t("common.password"),
            readOnly: !roles.includes(Roles.SELF_CHANGE_PASSWORD),
            fields: [
                {
                    component: "PrefPassword",
                    options: {}
                }
            ]
        }
    ];

    const categories = [
        {
            code: "main",
            name: vueI18N.t("common.general"),
            icon: "wrench",
            groups: mainCategoryGroups
        },
        {
            code: "advanced",
            name: vueI18N.t("common.advanced"),
            icon: "plus",
            groups: advancedCategoryGroups
        },
        {
            code: "security",
            name: vueI18N.t("common.security"),
            icon: "server",
            groups: securityCategoryGroups
        }
    ];

    if (roles.includes(Roles.HAS_CTI) && roles.includes(Roles.HAS_IM)) {
        const telephonyCategoryGroups = [
            {
                title: vueI18N.t("preferences.telephony.status"),
                fields: [
                    {
                        component: "PrefIMSetPhonePresence",
                        setting: "im_set_phone_presence",
                        options: {}
                    }
                ]
            }
        ];

        categories.push({
            code: "telephony",
            name: vueI18N.t("common.telephony"),
            icon: "cables",
            groups: telephonyCategoryGroups
        });
    }

    return {
        name: vueI18N.t("common.my_account"),
        href: "/",
        icon: { name: "preferences" },
        code: "my_account",
        categories
    };
}
