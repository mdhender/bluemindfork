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
        code: "main",
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
                        component: "PrefFieldSelect",
                        options: { choices: AvailablesTimezones.map(tz => ({ value: tz, text: tz })) }
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
                        options: {
                            label_enable_checkbox: vueI18N.t("preferences.advanced.notifications.enable_checkbox"),
                            label_enabled: vueI18N.t("preferences.advanced.notifications.enabled"),
                            label_disabled: vueI18N.t("preferences.advanced.notifications.disabled")
                        }
                    }
                ]
            }
        ]
    };

    const applicationSections = applications
        .filter(app => app.href === "/mail/")
        .map(a => {
            return {
                ...a,
                code: a.href.replaceAll("/", ""),
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
        });

    return [myAccount, ...applicationSections];
}
