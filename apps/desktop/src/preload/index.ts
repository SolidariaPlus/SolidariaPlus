import { contextBridge } from "electron";

// Minimal API exposed to the renderer; extend with ipcRenderer as needed.
contextBridge.exposeInMainWorld("solidaria", {
  platform: process.platform,
});
