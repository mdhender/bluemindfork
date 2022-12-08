module.exports = function (api, plugins = []) {
    api.cache(true);

    const presets = [
        [
            "@babel/preset-env",
            {
                modules: "auto",
                targets: {
                    browsers: ["defaults and not ie 11"],
                    node: 8
                }
            }
        ],
        "@babel/preset-typescript"
    ];

    if (process.env.NODE_ENV === "test") {
        plugins.push("require-context-hook");
    }

    return {
        presets,
        plugins,
        env: {
            test: {
                presets: [
                    [
                        "@babel/preset-env",
                        {
                            targets: {
                                node: "10"
                            }
                        }
                    ]
                ]
            }
        }
    };
};
