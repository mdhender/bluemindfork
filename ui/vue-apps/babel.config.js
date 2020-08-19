module.exports = function (api, plugins = []) {
    api.cache(true);

    const presets = [
        [
            "@babel/preset-env",
            {
                useBuiltIns: "usage",
                corejs: { version: 3, proposals: true }
            }
        ],
        "@babel/preset-typescript"
    ];

    if (process.env.NODE_ENV === "test") {
        plugins.push("require-context-hook");
    }

    return {
        presets,
        plugins
    };
};
