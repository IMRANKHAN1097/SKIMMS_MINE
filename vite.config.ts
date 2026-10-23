import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  // The repo's public/ folder holds Android Firebase config — don't publish it.
  publicDir: false,
});
