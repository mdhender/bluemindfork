process.env.TZ = "GMT";
process.env.LANG = "FR";

/** @type {import('jest').Config} */
const baseConfig = {
    testEnvironment: "jsdom",
    moduleFileExtensions: ["ts", "js", "json", "vue"],
    modulePathIgnorePatterns: ["open/clients/js/target/", "target"],
    setupFilesAfterEnv: ["./.jest/setupFilesAfterEnv.js", "fake-indexeddb/auto"],
    transform: {
        "^.+\\.[t|j]sx?$": "babel-jest",
        ".+\\.(css|styl|less|sass|scss|png|jpg|ttf|woff|woff2|svg)$": "jest-transform-stub",
        ".*\\.(vue)$": "@vue/vue2-jest"
    },
    transformIgnorePatterns: [
        "/node_modules/(?!(@bluemind/|storybook-addon-vue-info|storybook-addon-designs|workbox-.*))"
    ],
    snapshotFormat: {
        escapeString: true,
        printBasicPrototype: true
    },
    globals: {
        Uint8Array: Uint8Array,
        ArrayBuffer: ArrayBuffer
    }
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
                "^!css-loader!sass-loader!(.*)$": "$1.scss",
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
