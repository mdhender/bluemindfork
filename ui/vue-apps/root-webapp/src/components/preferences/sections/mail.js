import Roles from "@bluemind/roles";

import listStyleCompact from "../../../../assets/list-style-compact.png";
import listStyleFull from "../../../../assets/list-style-full.png";
import listStyleNormal from "../../../../assets/list-style-normal.png";
import threadSettingImageOn from "../../../../assets/setting-thread-on.svg";
import threadSettingImageOff from "../../../../assets/setting-thread-off.svg";

import { mapExtensions } from "@bluemind/extensions";

export default function (i18n) {
    const mail = mapExtensions("net.bluemind.webapp", ["application"]).application?.find(
        ({ $bundle }) => $bundle === "net.bluemind.webapp.mail.js"
    );
    return {
        id: "mail",
        name: i18n.t("common.application.webmail"),
        icon: mail?.icon,
        priority: mail?.priority,
        visible: { name: "RoleCondition", args: [Roles.HAS_MAIL] },
        categories: [main(i18n), myMailbox(i18n), otherMailboxes(i18n), filters(i18n), identities(i18n), advanced(i18n)]
    };
}

function main(i18n) {
    return {
        id: "main",
        name: i18n.t("common.general"),
        icon: "tool",
        groups: [
            {
                id: "thread",
                name: i18n.t("preferences.mail.thread"),
                disabled: { name: "StoreFieldCondition", args: ["mail.main.thread.field", "unavailable"] },
                fields: [
                    {
                        id: "field",
                        component: {
                            name: "PrefFieldChoice",
                            options: {
                                setting: "mail_thread",
                                default: "false",
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
                    },
                    {
                        id: "thread",
                        disabled: { name: "StoreFieldCondition", args: ["mail.main.thread.field", "false"] },
                        component: {
                            name: "PrefThread",
                            options: {
                                needReload: false,
                                autosave: true
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
                                default: "normal",
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
                id: "compose_new_window",
                name: i18n.t("preferences.mail.compose.title"),
                fields: [
                    {
                        id: "field",
                        component: {
                            name: "PrefFieldCheck",
                            options: {
                                setting: "mail_compose_in_new_window",
                                autosave: true,
                                label: i18n.t("preferences.mail.compose.in_new_window")
                            }
                        }
                    },
                    {
                        id: "default_font",
                        component: {
                            name: "PrefComposerDefaultFont",
                            options: {
                                setting: "composer_default_font",
                                default: "montserrat",
                                autosave: true,
                                label: i18n.t("preferences.mail.compose.default_font")
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
                                default: "true",
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
                                default: "false",
                                autosave: true,
                                label: i18n.t("preferences.mail.logout.empty.trash")
                            }
                        }
                    }
                ]
            },
            {
                id: "read_and_delivery_receipts",
                name: i18n.t("preferences.mail.receipts"),
                fields: [
                    {
                        id: "info",
                        component: {
                            name: "PrefFieldInfo",
                            options: {
                                lines: [
                                    i18n.t("preferences.mail.receipts.info.delivery"),
                                    i18n.t("preferences.mail.receipts.info.read")
                                ],
                                readMoreLink:
                                    "https://doc.bluemind.net/current/guide_de_l_utilisateur/la_messagerie/envoyer_un_message#dsn-mdn"
                            }
                        }
                    },
                    {
                        id: "ask",
                        component: {
                            name: "PrefFieldLabel",
                            options: {
                                label: i18n.t("preferences.mail.receipts.ask")
                            }
                        }
                    },
                    {
                        id: "ask_delivery",
                        component: {
                            name: "PrefFieldCheck",
                            options: {
                                setting: "always_ask_delivery_receipt",
                                default: "false",
                                autosave: true,
                                label: i18n.t("preferences.mail.receipts.ask.delivery")
                            }
                        }
                    },
                    {
                        id: "ask_read",
                        component: {
                            name: "PrefFieldCheck",
                            options: {
                                setting: "always_ask_read_receipt",
                                default: "false",
                                autosave: true,
                                label: i18n.t("preferences.mail.receipts.ask.read")
                            }
                        }
                    },
                    {
                        id: "answer_read_confirmation",
                        component: {
                            name: "PrefFieldLabel",
                            options: {
                                label: i18n.t("preferences.mail.receipts.answer_read_confirmation")
                            }
                        }
                    },
                    {
                        id: "answer_read_confirmation_choice",
                        component: {
                            name: "PrefFieldChoice",
                            options: {
                                setting: "answer_read_confirmation",
                                default: "ask",
                                autosave: true,
                                choices: [
                                    {
                                        name: i18n.t("preferences.mail.receipts.answer_read_confirmation.never"),
                                        value: "never"
                                    },
                                    {
                                        name: i18n.t("preferences.mail.receipts.answer_read_confirmation.ask"),
                                        value: "ask"
                                    },
                                    {
                                        name: i18n.t("preferences.mail.receipts.answer_read_confirmation.always"),
                                        value: "always"
                                    }
                                ]
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
                                default: "false",
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
                        visible: {
                            name: "QuotaCondition.hasLimit"
                        },
                        component: {
                            name: "PrefFieldSwitch",
                            options: {
                                setting: "always_show_quota",
                                default: "false",
                                autosave: true,
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
                id: "default_app",
                name: i18n.t("preferences.mail.mailto_links"),
                visible: { name: "RoleCondition", args: [Roles.HAS_MAIL_WEBAPP] },
                fields: [{ id: "default_app_action", component: { name: "PrefMailtoLinks" } }]
            }
        ]
    };
}

function myMailbox(i18n) {
    return {
        id: "my_mailbox",
        name: i18n.t("common.my_mailbox"),
        icon: "mail-user",
        groups: [
            {
                id: "group",
                name: i18n.t("common.my_mailbox"),
                fields: [{ id: "field", component: { name: "PrefManageMyMailbox" } }]
            }
        ]
    };
}

function otherMailboxes(i18n) {
    return {
        id: "other_mailboxes",
        name: i18n.t("common.other_mailboxes"),
        icon: "mail-3dots",
        groups: [
            {
                id: "group",
                name: i18n.t("common.other_mailboxes"),
                fields: [{ id: "field", component: { name: "PrefManageOtherMailboxes" } }]
            }
        ]
    };
}

function filters(i18n) {
    return {
        id: "filters",
        name: i18n.t("preferences.mail.filters"),
        icon: "funnel",
        groups: [
            {
                id: "filter",
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
    };
}

function identities(i18n) {
    return {
        id: "identities",
        name: i18n.t("common.identities"),
        icon: "pen",
        groups: [
            {
                id: "manage",
                name: i18n.t("common.identities"),
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
                                default: "false",
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
                                default: "never",
                                autosave: true,
                                label: {
                                    component: "PrefReadMoreLabel",
                                    options: {
                                        href: "https://doc.bluemind.net/release/5.1/guide_de_l_utilisateur/la_messagerie/gerer_les_identites_du_compte#param%C3%A9trer-le-champ-exp%C3%A9diteur",
                                        label: i18n.t("preferences.mail.identities.auto_select.label")
                                    }
                                },
                                choices: [
                                    { text: i18n.t("common.never"), value: "never" },
                                    {
                                        text: i18n.t("preferences.mail.identities.auto_select.only_for_replies"),
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
            },
            {
                id: "recipient-autocomplete",
                name: i18n.t("preferences.mail.advanced.recipient_autocomplete.title"),
                visible: { name: "RoleCondition", args: [Roles.HAS_MAIL_WEBAPP] },
                fields: [{ id: "reset", component: { name: "PrefDeleteRecipientPriorities" } }]
            }
        ]
    };
}
