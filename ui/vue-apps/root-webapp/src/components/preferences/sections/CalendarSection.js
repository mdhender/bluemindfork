export default function (vueI18N, applications) {
    return {
        name: vueI18N.t("common.application.calendar"),
        code: "calendar",
        icon: applications.find(({ $id }) => $id === "net.bluemind.webmodules.calendar")?.icon,
        categories: [mainCategory(vueI18N), myCalendarsCategory(vueI18N), otherCalendarsCategory(vueI18N)]
    };
}

function mainCategory(vueI18N) {
    return {
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
                        setting: "showweekends",
                        component: "PrefFieldCheck",
                        options: {
                            label: vueI18N.t("preferences.calendar.main.show_weekends")
                        }
                    },
                    {
                        component: "PrefWorkHours",
                        options: {}
                    },
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
                        setting: "show_declined_events",
                        component: "PrefFieldCheck",
                        options: {
                            label: vueI18N.t("preferences.calendar.main.show_declined_events")
                        }
                    }
                ]
            },
            {
                title: vueI18N.t("preferences.calendar.main.reminders"),
                fields: [
                    {
                        setting: "default_event_alert",
                        component: "PrefEventReminder",
                        options: {}
                    },
                    {
                        setting: "default_allday_event_alert",
                        component: "PrefAllDayEventReminder",
                        options: {}
                    },
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
}

function myCalendarsCategory(vueI18N) {
    return {
        code: "my_calendars",
        name: vueI18N.t("common.my_calendars"),
        icon: "event",
        groups: [
            {
                title: vueI18N.t("common.my_calendars"),
                fields: [
                    {
                        component: "PrefManageMyCalendars",
                        options: {}
                    }
                ]
            }
        ]
    };
}

function otherCalendarsCategory(vueI18N) {
    return {
        code: "other_calendars",
        name: vueI18N.t("common.other_calendars"),
        // FIXME same icon as myCalendar, ça va faire bizarre
        icon: "event",
        groups: [
            {
                title: vueI18N.t("common.other_calendars"),
                fields: [
                    {
                        component: "PrefManageOtherCalendars",
                        options: {}
                    }
                ]
            }
        ]
    };
}
