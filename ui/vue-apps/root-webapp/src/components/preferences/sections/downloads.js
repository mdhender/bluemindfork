import downloadIcon from "~/../assets/download-icon.js";

export default function (i18n) {
    return {
        name: i18n.t("preferences.downloads"),
        id: "downloads",
        icon: { svg: downloadIcon },
        categories: [
            {
                id: "main",
                name: i18n.t("common.general"),
                icon: "wrench",
                groups: [
                    {
                        id: "group",
                        fields: [
                            {
                                id: "field",
                                component: { name: "PrefDownloads" }
                            }
                        ]
                    }
                ]
            }
        ]
    };
}
