import Roles from "@bluemind/roles";

import listStyleCompact from "../../../../assets/list-style-compact.png";
import listStyleFull from "../../../../assets/list-style-full.png";
import listStyleNormal from "../../../../assets/list-style-normal.png";
import threadSettingImageOn from "../../../../assets/setting-thread-on.svg";
import threadSettingImageOff from "../../../../assets/setting-thread-off.svg";

import { mapExtensions } from "@bluemind/extensions";

import PrefSoonAvailable from "../PrefEntryName/PrefSoonAvailable";

export default function (i18n) {
    const mail = mapExtensions("webapp.banner", ["application"]).application?.find(
        ({ $id }) => $id === "net.bluemind.webapp.mail.js"
    );
    return {
        id: "mail",
        name: i18n.t("common.application.webmail"),
        icon: mail?.icon,
        priority: mail?.priority,
        visible: { name: "RoleCondition", args: [Roles.HAS_MAIL] },
        categories: [
            {
                id: "main",
                name: i18n.t("common.general"),
                icon: "wrench",
                groups: [
                    {
                        id: "thread",
                        name: i18n.t("preferences.mail.thread"),
                        nameRenderer: PrefSoonAvailable,
                        disabled: { name: "StoreFieldCondition", args: ["mail.main.thread.field", "unavailable"] },
                        fields: [
                            {
                                id: "field",
                                component: {
                                    name: "PrefFieldChoice",
                                    options: {
                                        setting: "mail_thread",
                                        needReload: true,
                                        choices: [
                                            {
                                                name: i18n.t("preferences.mail.thread.enable"),
                                                value: "true",
                                                svg: threadSettingImageOn
                                            },
                                            {
                                                name: i18n.t("preferences.mail.thread.disable"),
                                                value: "false",
                                                svg: threadSettingImageOff
                                            }
                                        ]
                                    }
                                }
                            }
                        ]
                    },
                    {
                        id: "list",
                        name: i18n.t("preferences.mail.message.list.display"),
                        fields: [
                            {
                                id: "field",
                                component: {
                                    name: "PrefFieldChoice",
                                    options: {
                                        setting: "mail_message_list_style",
                                        autosave: true,
                                        choices: [
                                            {
                                                name: i18n.t("preferences.mail.message.list.display.full"),
                                                value: "full",
                                                img: listStyleFull
                                            },
                                            {
                                                name: i18n.t("preferences.mail.message.list.display.normal"),
                                                value: "normal",
                                                img: listStyleNormal
                                            },
                                            {
                                                name: i18n.t("preferences.mail.message.list.display.compact"),
                                                value: "compact",
                                                img: listStyleCompact
                                            }
                                        ]
                                    }
                                }
                            }
                        ]
                    },
                    {
                        id: "signature",
                        name: i18n.t("common.signature"),
                        fields: [
                            {
                                id: "field",
                                component: {
                                    name: "PrefFieldSwitch",
                                    options: {
                                        setting: "insert_signature",
                                        autosave: true,
                                        label: i18n.t("preferences.mail.signature.insert")
                                    }
                                }
                            }
                        ]
                    },
                    {
                        id: "purge_on_logout",
                        name: i18n.t("preferences.mail.logout"),
                        fields: [
                            {
                                id: "field",
                                component: {
                                    name: "PrefFieldSwitch",
                                    options: {
                                        setting: "logout_purge",
                                        autosave: true,
                                        label: i18n.t("preferences.mail.logout.empty.trash")
                                    }
                                }
                            }
                        ]
                    },
                    {
                        id: "remote_images",
                        name: i18n.t("preferences.mail.remote.images"),
                        fields: [
                            {
                                id: "field",
                                component: {
                                    name: "PrefFieldSwitch",
                                    options: {
                                        setting: "trust_every_remote_content",
                                        autosave: true,
                                        additional_component: "PrefRemoteImage",
                                        label: i18n.t("preferences.mail.remote.images.trust")
                                    }
                                }
                            },
                            {
                                id: "help",
                                component: { name: "PrefRemoteImage" }
                            }
                        ]
                    },
                    {
                        id: "show_quota",
                        name: i18n.t("preferences.mail.quota"),
                        fields: [
                            {
                                id: "field",
                                component: {
                                    name: "PrefFieldSwitch",
                                    options: {
                                        autosave: true,
                                        setting: "always_show_quota",
                                        label: i18n.t("preferences.mail.quota.always.display")
                                    }
                                }
                            },
                            {
                                id: "usage",
                                component: { name: "PrefAlwaysShowQuota" }
                            }
                        ]
                    },
                    {
                        id: "automatic_reply",
                        name: i18n.t("preferences.mail.automatic_reply"),
                        visible: { name: "RoleCondition", args: [Roles.SELF_CHANGE_MAILBOX_FILTER] },
                        fields: [
                            {
                                id: "field",
                                component: { name: "PrefAutomaticReply" }
                            }
                        ]
                    },
                    {
                        id: "forwarding",
                        name: i18n.t("preferences.mail.emails_forwarding"),
                        visible: { name: "RoleCondition", args: [Roles.SELF_CHANGE_MAILBOX_FILTER] },
                        fields: [
                            {
                                id: "field",
                                component: { name: "PrefEmailsForwarding" }
                            }
                        ]
                    },
                    {
                        id: "filters",
                        name: i18n.t("preferences.mail.filters"),
                        description: i18n.t("preferences.mail.filters.desc"),
                        fields: [
                            {
                                id: "domain_filters",
                                visible: { name: "RoleCondition", args: [Roles.READ_DOMAIN_FILTERS] },
                                component: { name: "PrefDomainFilterRules" }
                            },
                            {
                                id: "my_filters",
                                visible: { name: "RoleCondition", args: [Roles.SELF_CHANGE_MAILBOX_FILTER] },
                                component: {
                                    name: "PrefMyFilterRules",
                                    options: { autosave: true }
                                }
                            }
                        ]
                    }
                ]
            },
            {
                id: "my_mailbox",
                name: i18n.t("common.my_mailbox"),
                icon: "user-enveloppe",
                groups: [
                    {
                        name: i18n.t("common.my_mailbox"),
                        id: "group",
                        fields: [{ id: "field", component: { name: "PrefManageMyMailbox" } }]
                    }
                ]
            },
            {
                id: "other_mailboxes",
                name: i18n.t("common.other_mailboxes"),
                icon: "3dots-enveloppe",
                groups: [
                    {
                        name: i18n.t("common.other_mailboxes"),
                        id: "group",
                        fields: [{ id: "field", component: { name: "PrefManageOtherMailboxes" } }]
                    }
                ]
            },
            {
                id: "identities",
                name: i18n.t("common.identities"),
                icon: "pen",
                groups: [
                    {
                        id: "manage",
                        name: i18n.t("preferences.mail.identities.manage"),
                        disabled: {
                            name: "RoleCondition.none",
                            args: [Roles.MANAGE_USER_MAIL_IDENTITIES, Roles.SELF_CHANGE_MAIL_IDENTITIES]
                        },
                        fields: [
                            {
                                id: "field",
                                component: {
                                    name: "PrefFieldSwitch",
                                    options: {
                                        setting: "always_show_from",
                                        autosave: true,
                                        label: i18n.t("preferences.mail.identities.always_show_from")
                                    }
                                }
                            },
                            {
                                id: "automatic",
                                component: {
                                    name: "PrefFieldSelect",
                                    options: {
                                        setting: "auto_select_from",
                                        autosave: true,
                                        label: {
                                            component: "PrefReadMoreLabel",
                                            options: {
                                                href:
                                                    "https://doc.bluemind.net/release/5.0/Guide_de_l_utilisateur/La_messagerie/Gerer_les_identites_du_compte#param%C3%A9trer-le-champ-exp%C3%A9diteur",
                                                text: i18n.t("preferences.mail.identities.auto_select.label")
                                            }
                                        },
                                        choices: [
                                            { text: i18n.t("common.never"), value: "never" },
                                            {
                                                text: i18n.t(
                                                    "preferences.mail.identities.auto_select.only_for_replies"
                                                ),
                                                value: "only_replies"
                                            },
                                            {
                                                text: i18n.t("preferences.mail.identities.auto_select.replies_and_new"),
                                                value: "replies_and_new_messages"
                                            }
                                        ]
                                    }
                                }
                            },
                            {
                                id: "manage",
                                component: { name: "PrefManageIdentities" }
                            }
                        ]
                    }
                ]
            },
            {
                id: "advanced",
                name: i18n.t("common.advanced"),
                icon: "plus",
                priority: -1,
                groups: [
                    {
                        id: "application",
                        name: i18n.t("preferences.mail.advanced.switch.title"),
                        visible: { name: "RoleCondition.every", args: [Roles.HAS_WEBMAIL, Roles.HAS_MAIL_WEBAPP] },
                        fields: [
                            {
                                id: "field",
                                component: {
                                    name: "PrefFieldSwitch",
                                    options: {
                                        setting: "mail-application",
                                        label: i18n.t("preferences.mail.advanced.switch.label"),
                                        checkedValue: "mail-webapp",
                                        uncheckedValue: "webmail",
                                        needReload: true
                                    }
                                }
                            },
                            {
                                id: "image",
                                disabled: {
                                    name: "StoreFieldCondition",
                                    args: ["mail.advanced.application.field", "webmail"]
                                },
                                component: {
                                    name: "PrefSwitchWebmail"
                                }
                            }
                        ]
                    }
                ]
            }
        ]
    };
}
