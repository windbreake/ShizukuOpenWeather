# Platform Notes

The MVP targets desktop web on Linux development containers, while keeping future Windows, macOS, Linux, mobile web, and desktop app support practical.

Rules:

- Do not hard-code Linux-only paths in business code.
- Keep database paths configurable.
- Use Rust `directories` for CLI config paths.
- Use Java `Path` APIs instead of string path concatenation.
- Keep the Vue app browser-only and independent from Electron APIs.
- Keep REST contracts stable for future desktop/mobile clients.
