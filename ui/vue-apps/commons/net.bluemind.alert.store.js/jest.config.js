process.env.TZ = "GMT";

module.exports = {
    name: "@bluemind/alert.store",
    displayName: "BlueMind alert module : Vuex model",
    verbose: true,
    moduleFileExtensions: ["ts", "js", "json", "vue"],
    modulePathIgnorePatterns: ["open/clients/js/target/", "target"],
    transform: {
        "^.+\\.[t|j]sx?$": "babel-jest",
        ".+\\.(css|styl|less|sass|scss|png|jpg|ttf|woff|woff2|svg)$": "jest-transform-stub",
        ".*\\.(vue)$": "vue-jest"
    },
    transformIgnorePatterns: [
        "/node_modules/(?!(@bluemind/|storybook-addon-vue-info|storybook-addon-designs|workbox-.*))"
    ],
    testURL: "http://localhost",
    setupFilesAfterEnv: ["<rootDir>/../../.jest/register-context.js", "fake-indexeddb/auto"]
};
