// Expo SDK 52+ detecta monorepos automáticamente (watchFolders / nodeModulesPaths).
const { getDefaultConfig } = require("expo/metro-config");

module.exports = getDefaultConfig(__dirname);
