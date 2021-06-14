import { AvailableTimeFormats, AvailableDateFormats, AvailablesTimezones } from "@bluemind/date";
import { AvailableLanguages } from "@bluemind/i18n";
import Roles from "@bluemind/roles";

import NotificationManager from "../../NotificationManager";

import listStyleCompact from "../../../assets/list-style-compact.png";
import listStyleFull from "../../../assets/list-style-full.png";
import listStyleNormal from "../../../assets/list-style-normal.png";
import threadSettingImageOn from "../../../assets/setting-thread-on.svg";
import threadSettingImageOff from "../../../assets/setting-thread-off.svg";

/**
 * Here is the 'heart' of the Settings.
 * Each section, like "mail", holds several categories, like "main" or "advanced". Each category holds groups of fields.
 * These fields are created using Dynamic Components (see PrefContent).
 */
export default function (applications, roles, vueI18N) {
    const sections = [getMyAccountSection(roles, vueI18N)];

    if (roles.includes(Roles.HAS_MAIL)) {
        sections.push(getWebmailSection(roles, vueI18N, applications));
    }
    if (roles.includes(Roles.HAS_CALENDAR)) {
        sections.push(getCalendarSection(vueI18N, applications));
    }

    return sections;
}

function getMyAccountSection(roles, vueI18N) {
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

function getWebmailSection(roles, vueI18N, applications) {
    return {
        name: vueI18N.t("common.application.webmail"),
        code: "mail",
        icon: applications.find(({ $id }) => $id === "net.bluemind.webapp.mail.js")?.icon,
        categories: [
            {
                code: "main",
                name: vueI18N.t("common.general"),
                icon: "wrench",
                groups: [
                    {
                        title: vueI18N.t("preferences.mail.thread"),
                        availableSoon: true,
                        fields: [
                            {
                                component: "PrefFieldChoice",
                                setting: "mail_thread",
                                options: {
                                    choices: [
                                        {
                                            name: vueI18N.t("preferences.mail.thread.enable"),
                                            value: "true",
                                            svg: threadSettingImageOn
                                        },
                                        {
                                            name: vueI18N.t("preferences.mail.thread.disable"),
                                            value: "false",
                                            svg: threadSettingImageOff
                                        }
                                    ]
                                }
                            }
                        ]
                    },
                    {
                        title: vueI18N.t("preferences.mail.message.list.display"),
                        fields: [
                            {
                                component: "PrefFieldChoice",
                                setting: "mail_message_list_style",
                                options: {
                                    choices: [
                                        {
                                            name: vueI18N.t("preferences.mail.message.list.display.full"),
                                            value: "full",
                                            img: listStyleFull
                                        },
                                        {
                                            name: vueI18N.t("preferences.mail.message.list.display.normal"),
                                            value: "normal",
                                            img: listStyleNormal
                                        },
                                        {
                                            name: vueI18N.t("preferences.mail.message.list.display.compact"),
                                            value: "compact",
                                            img: listStyleCompact
                                        }
                                    ]
                                }
                            }
                        ]
                    },
                    {
                        title: vueI18N.t("common.signature"),
                        fields: [
                            {
                                component: "PrefFieldCheck",
                                setting: "insert_signature",
                                options: {
                                    label: vueI18N.t("preferences.mail.signature.insert")
                                }
                            }
                        ]
                    },
                    {
                        title: vueI18N.t("preferences.mail.logout"),
                        fields: [
                            {
                                component: "PrefFieldCheck",
                                setting: "logout_purge",
                                options: {
                                    label: vueI18N.t("preferences.mail.logout.empty.trash")
                                }
                            }
                        ]
                    },
                    {
                        title: vueI18N.t("preferences.mail.remote.images"),
                        fields: [
                            {
                                component: "PrefFieldCheck",
                                setting: "trust_every_remote_content",
                                options: {
                                    additional_component: "PrefRemoteImage",
                                    label: vueI18N.t("preferences.mail.remote.images.trust")
                                }
                            }
                        ]
                    },
                    {
                        title: vueI18N.t("preferences.mail.quota"),
                        fields: [
                            {
                                component: "PrefFieldCheck",
                                setting: "always_show_quota",
                                options: {
                                    additional_component: "PrefAlwaysShowQuota",
                                    label: vueI18N.t("preferences.mail.quota.always.display")
                                }
                            }
                        ]
                    }
                ]
            },
            {
                code: "identities",
                name: vueI18N.t("common.identities"),
                icon: "pen",
                groups: [
                    {
                        title: vueI18N.t("preferences.mail.identities.manage"),
                        readOnly: !roles.includes(Roles.MANAGE_MAILBOX_IDENTITIES),
                        fields: [
                            {
                                component: "PrefManageIdentities",
                                setting: "always_show_from",
                                options: {}
                            }
                        ]
                    }
                ]
            }
        ]
    };
}

function getCalendarSection(vueI18N, applications) {
    return {
        name: vueI18N.t("common.application.calendar"),
        code: "calendar",
        icon: applications.find(({ $id }) => $id === "net.bluemind.webmodules.calendar")?.icon,
        categories: [
            {
                code: "main",
                name: vueI18N.t("common.general"),
                icon: "wrench",
                groups: [
                    {
                        title: vueI18N.t("preferences.calendar.main.configure_view"),
                        fields: [
                            {
                                name: vueI18N.t("preferences.calendar.main.week_starts_on"),
                                setting: "day_weekstart",
                                component: "PrefFieldSelect",
                                options: {
                                    choices: [
                                        { text: vueI18N.t("common.monday"), value: "monday" },
                                        { text: vueI18N.t("common.sunday"), value: "sunday" }
                                    ]
                                }
                            },
                            {
                                name: vueI18N.t("preferences.calendar.main.default_view"),
                                setting: "defaultview",
                                component: "PrefFieldSelect",
                                options: {
                                    choices: [
                                        { text: vueI18N.t("common.day"), value: "day" },
                                        { text: vueI18N.t("common.week"), value: "week" },
                                        { text: vueI18N.t("common.month"), value: "month" },
                                        { text: vueI18N.t("common.list"), value: "agenda" }
                                    ]
                                }
                            },
                            {
                                name: vueI18N.t("preferences.calendar.main.show_weekends"),
                                setting: "showweekends",
                                component: "PrefFieldCheck",
                                options: {
                                    label: vueI18N.t("preferences.calendar.main.show_weekends")
                                }
                            },
                            {
                                name: vueI18N.t("preferences.calendar.main.day_starts_at"),
                                setting: "work_hours_start",
                                component: "PrefWorksHours",
                                options: {}
                            },
                            {
                                name: vueI18N.t("preferences.calendar.main.day_ends_at"),
                                setting: "work_hours_end",
                                component: "PrefWorksHours",
                                options: {}
                            },
                            // FIXME: do we keep the same UX for this field ?
                            //      in old settings app when you check this option, it disabled 2 previous fields and it forces its value to O
                            // {
                            //     name: vueI18N.t("preferences.calendar.main.whole_day"),
                            //     setting: "",
                            //     component: "PrefFieldCheck",
                            //     options: {
                            //         label: vueI18N.t("preferences.calendar.main.whole_day")
                            //     }
                            // }

                            //FIXME: besoin de maquettes pour voir quel rendu on veut pour un multiple-select
                            // {
                            //     name: vueI18N.t("preferences.calendar.main.working_days"),
                            //     setting: "working_days",
                            //     component: "PrefFieldSelect",
                            //     options: {
                            //            choices: []
                            //     }
                            // }

                            {
                                name: vueI18N.t("preferences.calendar.main.show_declined_events"),
                                setting: "show_declined_events",
                                component: "PrefFieldCheck",
                                options: {
                                    label: vueI18N.t("preferences.calendar.main.show_declined_events")
                                }
                            }
                        ]
                    },
                    {
                        title: vueI18N.t("preferences.calendar.main.reminder"),
                        fields: [
                            //FIXME: comment on gère le bouton "Désactiver" ?
                            // {
                            //     name: vueI18N.t("preferences.calendar.main.default_reminder"),
                            //     setting: "default_event_alert_mode",
                            //     component: "",
                            //     options: {}
                            // },
                            // {
                            //     name: vueI18N.t("preferences.calendar.main.default_allday_reminder"),
                            //     setting: "default_allday_event_alert",
                            //     component: "",
                            //     options: {}
                            // },

                            {
                                name: vueI18N.t("preferences.calendar.main.default_reminder_kind"),
                                setting: "default_event_alert_mode",
                                component: "PrefFieldSelect",
                                options: {
                                    choices: [
                                        { text: vueI18N.t("common.email"), value: "Email" },
                                        { text: vueI18N.t("common.notification"), value: "Display" }
                                    ]
                                }
                            }
                        ]
                    }
                ]
            }
        ]
    };
}
