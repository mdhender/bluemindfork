process.env.TZ = "GMT";
process.env.LANG = "FR";

const baseConfig = {
    verbose: true,
    moduleFileExtensions: ["ts", "js", "json", "vue"],
    modulePathIgnorePatterns: ["open/clients/js/target/", "target"],
    setupFilesAfterEnv: ["./.jest/setupFilesAfterEnv.js", "fake-indexeddb/auto"],
    transform: {
        "^.+\\.[t|j]sx?$": "babel-jest",
        ".+\\.(css|styl|less|sass|scss|png|jpg|ttf|woff|woff2|svg)$": "jest-transform-stub",
        ".*\\.(vue)$": "vue-jest"
    },
    transformIgnorePatterns: [
        "/node_modules/(?!(@bluemind/|storybook-addon-vue-info|storybook-addon-designs|workbox-.*))"
    ]
};

const testFileMatcher = "**/*.spec.(js|ts)";

module.exports = {
    projects: [
        {
            ...baseConfig,
            displayName: "commons",
            testMatch: ["<rootDir>/commons/" + testFileMatcher]
        },
        {
            ...baseConfig,
            displayName: "plugins",
            testMatch: ["<rootDir>/plugins/" + testFileMatcher]
        },
        {
            ...baseConfig,
            displayName: "mail-app",
            moduleNameMapper: {
                "^~/actions$": "<rootDir>/mail-webapp/src/store/types/actions",
                "^~/getters$": "<rootDir>/mail-webapp/src/store/types/getters",
                "^~/mutations$": "<rootDir>/mail-webapp/src/store/types/mutations",
                "~(.*)$": "<rootDir>/mail-webapp/src$1"
            },
            testMatch: ["<rootDir>/mail-webapp/" + testFileMatcher]
        },
        {
            ...baseConfig,
            displayName: "root-app",
            moduleNameMapper: {
                "~(.*)$": "<rootDir>/root-webapp/src$1"
            },
            testMatch: ["<rootDir>/root-webapp/" + testFileMatcher]
        }
    ]
};
