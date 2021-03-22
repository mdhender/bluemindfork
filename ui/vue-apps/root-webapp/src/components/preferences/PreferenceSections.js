import { AvailableTimeFormats, AvailableDateFormats, AvailablesTimezones } from "@bluemind/date";
import { AvailableLanguages } from "@bluemind/i18n";

import NotificationManager from "../../NotificationManager";

import listStyleCompact from "../../../assets/list-style-compact.png";
import listStyleFull from "../../../assets/list-style-full.png";
import listStyleNormal from "../../../assets/list-style-normal.png";
import threadSettingImageOn from "../../../assets/setting-thread-on.svg";
import threadSettingImageOff from "../../../assets/setting-thread-off.svg";

/**
 * Here is the 'heart' of the Settings.
 * Each section, like "mail", holds several categories, like "main". Each category holds fields.
 * These fields are created using Dynamic Components (see PrefContent).
 */
export default function (applications, vueI18N) {
    const availableDefaultApps = [
        { text: vueI18N.t("common.application.webmail"), value: "/webmail/" },
        { text: vueI18N.t("common.application.calendar"), value: "/cal/" }
    ];

    const myAccount = {
        name: vueI18N.t("common.my_account"),
        href: "/",
        icon: { name: "preferences" },
        code: "my_account",
        categories: [
            {
                code: "main",
                name: vueI18N.t("common.general"),
                icon: "wrench",
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
                    },
                    {
                        name: vueI18N.t("preferences.general.default_application"),
                        setting: "default_app",
                        component: "PrefFieldSelect",
                        options: { choices: availableDefaultApps }
                    }
                ]
            },
            {
                code: "advanced",
                name: vueI18N.t("common.advanced"),
                icon: "plus",
                fields: [
                    {
                        name: vueI18N.t("preferences.advanced.reinit_local_data"),
                        component: "PrefResetLocalData",
                        options: {
                            text: vueI18N.t("common.action.reset"),
                            label: vueI18N.t("preferences.advanced.reinit_local_data.explanations")
                        }
                    },
                    {
                        name: vueI18N.t("preferences.advanced.notifications"),
                        component: "PrefEnableNotifications",
                        condition: new NotificationManager().isAvailable,
                        options: {}
                    }
                ]
            }
        ]
    };

    const webmail = {
        name: vueI18N.t("common.application.webmail"),
        code: "mail",
        icon: applications.find(app => app.href === "/mail/").icon,
        categories: [
            {
                code: "main",
                name: vueI18N.t("common.general"),
                icon: "wrench",
                fields: [
                    {
                        component: "PrefFieldChoice",
                        name: vueI18N.t("preferences.mail.thread"),
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
                        },
                        availableSoon: true
                    },
                    {
                        component: "PrefFieldChoice",
                        name: vueI18N.t("preferences.mail.message.list.display"),
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
                    },
                    {
                        component: "PrefFieldCheck",
                        name: vueI18N.t("preferences.mail.signature"),
                        setting: "insert_signature",
                        options: {
                            label: vueI18N.t("preferences.mail.signature.insert")
                        }
                    },
                    {
                        component: "PrefFieldCheck",
                        name: vueI18N.t("preferences.mail.logout"),
                        setting: "logout_purge",
                        options: {
                            label: vueI18N.t("preferences.mail.logout.empty.trash")
                        }
                    },
                    {
                        component: "PrefFieldCheck",
                        name: vueI18N.t("preferences.mail.remote.images"),
                        setting: "trust_every_remote_content",
                        options: {
                            additional_component: "PrefRemoteImage",
                            label: vueI18N.t("preferences.mail.remote.images.trust")
                        }
                    },
                    {
                        component: "PrefFieldCheck",
                        name: vueI18N.t("preferences.mail.quota"),
                        setting: "always_show_quota",
                        options: {
                            additional_component: "PrefAlwaysShowQuota",
                            label: vueI18N.t("preferences.mail.quota.always.display")
                        }
                    }
                ]
            }
        ]
    };

    const calendar = {
        name: vueI18N.t("common.application.calendar"),
        code: "calendar",
        icon: applications.find(app => app.href === "/cal/").icon,
        categories: [
            {
                code: "main",
                name: vueI18N.t("common.general"),
                icon: "wrench",
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
                    },

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
    };

    return [myAccount, webmail, calendar];
}
