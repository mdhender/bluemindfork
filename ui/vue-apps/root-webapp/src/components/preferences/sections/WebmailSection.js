import Roles from "@bluemind/roles";

import listStyleCompact from "../../../../assets/list-style-compact.png";
import listStyleFull from "../../../../assets/list-style-full.png";
import listStyleNormal from "../../../../assets/list-style-normal.png";
import threadSettingImageOn from "../../../../assets/setting-thread-on.svg";
import threadSettingImageOff from "../../../../assets/setting-thread-off.svg";
import mailAppVersionSettingImageClassic from "../../../../assets/setting-mail-app-version-classic.png";
import mailAppVersionSettingImageModern from "../../../../assets/setting-mail-app-version-modern.png";

export default function (roles, vueI18N, applications) {
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
                        notAvailable: ["mail_thread", "unavailable"],
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
                    },
                    {
                        title: vueI18N.t("preferences.mail.automatic_reply"),
                        fields: [
                            {
                                component: "PrefAutomaticReply",
                                options: {}
                            }
                        ]
                    },
                    {
                        title: vueI18N.t("preferences.mail.emails_forwarding"),
                        fields: [
                            {
                                component: "PrefEmailsForwarding",
                                options: {}
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
                        readOnly:
                            !roles.includes(Roles.MANAGE_MAILBOX_IDENTITIES) &&
                            !roles.includes(Roles.SELF_CHANGE_MAIL_IDENTITIES),
                        fields: [
                            {
                                component: "PrefManageIdentities",
                                setting: "always_show_from",
                                options: {}
                            }
                        ]
                    }
                ]
            },
            {
                code: "advanced",
                name: vueI18N.t("common.advanced"),
                icon: "plus",
                groups: [
                    {
                        title: vueI18N.t("preferences.mail.advanced.switch"),
                        readOnly: !roles.includes(Roles.HAS_WEBMAIL) && !roles.includes(Roles.HAS_MAIL_WEBAPP),
                        fields: [
                            {
                                component: "PrefFieldSwitch",
                                setting: "mail-application",
                                options: {
                                    autosave: true,
                                    false: {
                                        name: vueI18N.t("preferences.mail.advanced.switch.classic"),
                                        value: "webmail",
                                        image: mailAppVersionSettingImageClassic,
                                        desc: vueI18N.t("preferences.mail.advanced.switch.classic.desc")
                                    },
                                    true: {
                                        name: vueI18N.t("preferences.mail.advanced.switch.modern"),
                                        value: "mail-webapp",
                                        image: mailAppVersionSettingImageModern,
                                        desc: vueI18N.t("preferences.mail.advanced.switch.modern.desc")
                                    }
                                }
                            }
                        ]
                    }
                ]
            }
        ]
    };
}
