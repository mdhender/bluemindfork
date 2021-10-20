import { mapExtensions } from "@bluemind/extensions";
import downloadIcon from "../../../../assets/download-icon.js";

export default function (roles, vueI18N) {
    return {
        name: vueI18N.t("preferences.downloads"),
        code: "downloads",
        icon: { svg: downloadIcon },
        categories: [
            {
                code: "main",
                name: vueI18N.t("common.general"),
                icon: "wrench",
                groups: [
                    {
                        fields: [
                            {
                                component: "PrefDownloads",
                                options: {
                                    downloads:
                                        mapExtensions("net.bluemind.ui.settings.downloads", {
                                            downloads: ({ download }) => {
                                                if (roles.includes(download.role)) {
                                                    return {
                                                        title: download.title,
                                                        desc: download.description,
                                                        url: download.url
                                                    };
                                                }
                                            }
                                        }).downloads || []
                                }
                            }
                        ]
                    }
                ]
            }
        ]
    };
}
