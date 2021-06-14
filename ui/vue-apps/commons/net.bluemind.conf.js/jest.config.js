process.env.TZ = "GMT";

module.exports = {
    rootDir: process.cwd() + "/",
    verbose: true,
    moduleFileExtensions: ["ts", "js", "json", "vue"],
    modulePathIgnorePatterns: ["open/clients/js/target/", "target"],
    transform: {
        "^.+\\.[t|j]sx?$": "babel-jest",
        ".+\\.(css|styl|less|sass|scss|png|jpg|ttf|woff|woff2|svg)$": "jest-transform-stub",
        ".*\\.(vue)$": "vue-jest"
    },
    moduleNameMapper: {
        "^~mixins$": "<rootDir>/mail-webapp/src/mixins",
        "^~model/attachment$": "<rootDir>/mail-webapp/src/model/attachment",
        "^~model/draft$": "<rootDir>/mail-webapp/src/model/draft",
        "^~model/folder$": "<rootDir>/mail-webapp/src/model/folder",
        "^~model/mailbox$": "<rootDir>/mail-webapp/src/model/mailbox",
        "^~model/message$": "<rootDir>/mail-webapp/src/model/message",
        "^~model/part$": "<rootDir>/mail-webapp/src/model/part",
        "^~model/signature$": "<rootDir>/mail-webapp/src/model/signature",
        "^~actions$": "<rootDir>/mail-webapp/src/store/types/actions",
        "^~getters$": "<rootDir>/mail-webapp/src/store/types/getters",
        "^~mutations$": "<rootDir>/mail-webapp/src/store/types/mutations"
    },
    transformIgnorePatterns: [
        "/node_modules/(?!(@bluemind/|storybook-addon-vue-info|storybook-addon-designs|workbox-.*))"
    ],
    testURL: "http://localhost",
    setupFilesAfterEnv: ["./.jest/register-context.js", "fake-indexeddb/auto"]
};
