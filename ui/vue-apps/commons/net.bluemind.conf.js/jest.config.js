process.env.TZ = "GMT";

module.exports = {
    rootDir: process.cwd() + "/",
    verbose: true,
    moduleFileExtensions: ["ts", "js", "json", "vue"],
    modulePathIgnorePatterns: ["open/clients/js/target/"],
    transform: {
        "^.+\\.[t|j]sx?$": "babel-jest",
        ".+\\.(css|styl|less|sass|scss|png|jpg|ttf|woff|woff2|svg)$": "jest-transform-stub",
        ".*\\.(vue)$": "vue-jest"
    },
    transformIgnorePatterns: ["/node_modules/(?!@bluemind/|storybook-addon-vue-info|storybook-addon-designs)"],
    testURL: "http://localhost",
    setupFiles: ["./.jest/register-context.js"]
};
