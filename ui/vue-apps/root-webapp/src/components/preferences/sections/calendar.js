import { AvailablesTimezones } from "@bluemind/date";
import Roles from "@bluemind/roles";
import { mapExtensions } from "@bluemind/extensions";

export default function (i18n) {
    const calendar = mapExtensions("net.bluemind.webapp", ["application"]).application?.find(
        ({ $bundle }) => $bundle === "net.bluemind.webmodules.calendar"
    );

    return {
        name: i18n.t("common.application.calendar"),
        id: "calendar",
        icon: calendar?.icon,
        priority: calendar?.priority,
        visible: { name: "RoleCondition", args: [Roles.HAS_CALENDAR] },
        categories: [mainCategory(i18n), myCalendarsCategory(i18n), otherCalendarsCategory(i18n)]
    };
}

function mainCategory(i18n) {
    return {
        id: "main",
        name: i18n.t("common.general"),
        icon: "wrench",
        groups: [
            {
                id: "view",
                name: i18n.t("preferences.calendar.main.configure_view"),
                fields: [
                    {
                        id: "day_weekstart",
                        component: {
                            name: "PrefFieldSelect",
                            options: {
                                setting: "day_weekstart",
                                needReload: true,
                                label: i18n.t("preferences.calendar.main.week_starts_on"),
                                choices: [
                                    { text: i18n.t("common.monday"), value: "monday" },
                                    { text: i18n.t("common.sunday"), value: "sunday" }
                                ]
                            }
                        }
                    },
                    {
                        id: "defaultview",
                        component: {
                            name: "PrefFieldSelect",
                            options: {
                                setting: "defaultview",
                                label: i18n.t("preferences.calendar.main.default_view"),
                                choices: [
                                    { text: i18n.t("common.day"), value: "day" },
                                    { text: i18n.t("common.week"), value: "week" },
                                    { text: i18n.t("common.month"), value: "month" },
                                    { text: i18n.t("common.list"), value: "agenda" }
                                ]
                            }
                        }
                    },
                    {
                        id: "showweekends",
                        component: {
                            name: "PrefFieldCheck",
                            options: {
                                setting: "showweekends",
                                needReload: true,
                                label: i18n.t("preferences.calendar.main.show_weekends")
                            }
                        }
                    },
                    {
                        id: "workhours",
                        component: { name: "PrefWorkHours", options: { needReload: true } }
                    },
                    {
                        id: "show_declined_events",
                        component: {
                            name: "PrefFieldCheck",
                            options: {
                                setting: "show_declined_events",
                                needReload: true,
                                label: i18n.t("preferences.calendar.main.show_declined_events")
                            }
                        }
                    },
                    {
                        id: "working_days",
                        component: {
                            name: "PrefWorkingDays",
                            options: {
                                setting: "working_days",
                                needReload: true,
                                choices: [
                                    { value: "mon", text: i18n.t("common.monday_short") },
                                    { value: "tue", text: i18n.t("common.tuesday_short") },
                                    { value: "wed", text: i18n.t("common.wednesday_short") },
                                    { value: "thu", text: i18n.t("common.thursday_short") },
                                    { value: "fri", text: i18n.t("common.friday_short") },
                                    { value: "sat", text: i18n.t("common.saturday_short") },
                                    { value: "sun", text: i18n.t("common.sunday_short") }
                                ]
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
                                setting: "timezone",
                                needReload: true
                            }
                        }
                    }
                ]
            },
            {
                id: "reminders",
                name: i18n.t("preferences.calendar.main.reminders"),
                fields: [
                    {
                        id: "default",
                        component: {
                            name: "PrefEventReminder",
                            options: { setting: "default_event_alert", needReload: true }
                        }
                    },
                    {
                        id: "default_allday",
                        component: {
                            name: "PrefAllDayEventReminder",
                            options: { setting: "default_allday_event_alert", needReload: true }
                        }
                    },
                    {
                        id: "default_mode",
                        component: {
                            name: "PrefFieldSelect",
                            options: {
                                setting: "default_event_alert_mode",
                                default: "Display",
                                label: i18n.t("preferences.calendar.main.default_reminder_kind"),
                                needReload: true,
                                choices: [
                                    { text: i18n.t("common.email"), value: "Email" },
                                    { text: i18n.t("common.notification"), value: "Display" }
                                ]
                            }
                        }
                    }
                ]
            }
        ]
    };
}

function myCalendarsCategory(i18n) {
    return {
        id: "my_calendars",
        name: i18n.t("common.my_calendars"),
        icon: "user-calendar",
        groups: [
            {
                id: "group",
                name: i18n.t("common.my_calendars"),
                fields: [
                    {
                        id: "field",
                        component: { name: "PrefManageMyCalendars" },
                        keywords: [i18n.t("preferences.calendar.my_calendars.availabilities_advanced_management")]
                    }
                ]
            }
        ]
    };
}

function otherCalendarsCategory(i18n) {
    return {
        id: "other_calendars",
        name: i18n.t("common.other_calendars"),
        icon: "3dots-calendar",
        groups: [
            {
                id: "group",
                name: i18n.t("common.other_calendars"),
                fields: [
                    {
                        id: "field",
                        component: { name: "PrefManageOtherCalendars" }
                    }
                ]
            }
        ]
    };
}
