const path = require('path')
const VueLoaderPlugin = require('vue-loader/lib/plugin')
const shouldMinimize = process.env["MINIMIZE"] === 'true'

module.exports = {
  entry: './app/main.js',
  output: {
    path: path.resolve(__dirname, '../../../target/generated-resources/vue-app'),
    publicPath: '/vue-components/dist/', filename: 'ui.js'
  },
  module: {
    rules: [
      { test: /\.css$/, use: [ 'vue-style-loader', 'css-loader' ] },
      { test: /\.vue$/, loader: 'vue-loader', options: { loaders: {} } },
      { test: /\.js$/, exclude: /node_modules/, use: { loader: 'babel-loader' } },
    ]
  },
  resolve: {
    alias: {
      'vue$': 'vue/dist/vue.esm.js'
    },
    extensions: ['*', '.js', '.vue', '.json']
  },
  optimization: {
      minimize: shouldMinimize
  },
  mode: 'production',
  plugins: [
    new VueLoaderPlugin()
  ]
};
