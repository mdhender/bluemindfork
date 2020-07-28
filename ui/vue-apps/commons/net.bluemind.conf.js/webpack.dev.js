const merge = require("webpack-merge");
const common = require("./webpack.common.js");

module.exports = merge(common, {
    mode: "development",
    devServer: {
        historyApiFallback: true,
        overlay: true,
        host: "0.0.0.0",
        disableHostCheck: true
    },
    devtool: "eval-source-map"
});
