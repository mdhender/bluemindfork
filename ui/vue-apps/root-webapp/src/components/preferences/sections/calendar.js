import Roles from "@bluemind/roles";
import { mapExtensions } from "@bluemind/extensions";

export default function (i18n) {
    const calendar = mapExtensions("webapp.banner", ["application"]).application?.find(
        ({ $id }) => $id === "net.bluemind.webmodules.calendar"
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
                                label: i18n.t("preferences.calendar.main.show_weekends")
                            }
                        }
                    },
                    {
                        id: "workhours",
                        component: { name: "PrefWorkHours" }
                    },
                    {
                        id: "show_declined_events",
                        component: {
                            name: "PrefFieldCheck",
                            options: {
                                setting: "show_declined_events",
                                label: i18n.t("preferences.calendar.main.show_declined_events")
                            }
                        }
                    },
                    {
                        id: "working_days",
                        component: {
                            name: "PrefFieldMultiSelect",
                            options: {
                                setting: "working_days",
                                label: i18n.t("preferences.calendar.main.working_days"),
                                choices: [
                                    { value: "mon", text: i18n.t("common.monday") },
                                    { value: "tue", text: i18n.t("common.tuesday") },
                                    { value: "wed", text: i18n.t("common.wednesday") },
                                    { value: "thu", text: i18n.t("common.thursday") },
                                    { value: "fri", text: i18n.t("common.friday") },
                                    { value: "sat", text: i18n.t("common.saturday") },
                                    { value: "sun", text: i18n.t("common.sunday") }
                                ],
                                selectAllLabel: i18n.t("preferences.calendar.main.working_days.all"),
                                placeholder: i18n.t("preferences.calendar.main.working_days.none")
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
                            options: { setting: "default_event_alert" }
                        }
                    },
                    {
                        id: "default_allday",
                        component: {
                            name: "PrefAllDayEventReminder",
                            options: { setting: "default_allday_event_alert" }
                        }
                    },
                    {
                        id: "default_mode",
                        component: {
                            name: "PrefFieldSelect",
                            options: {
                                label: i18n.t("preferences.calendar.main.default_reminder_kind"),
                                setting: "default_event_alert_mode",
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
