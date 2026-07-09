# Platform Notes

The MVP targets desktop web on Linux development containers, while keeping future Windows, macOS, Linux, mobile web, and desktop app support practical.

Rules:

- Do not hard-code Linux-only paths in business code.
- Keep database paths configurable.
- Use Rust `directories` for CLI config paths.
- Use Java `Path` APIs instead of string path concatenation.
- Keep the Vue app browser-only and independent from Electron APIs.
- Keep REST contracts stable for future desktop/mobile clients.

## Container And Proxy Isolation

The default development container should stay isolated:

- No host networking by default.
- No Docker socket mount.
- No broad host directory mounts.
- No committed proxy credentials.
- Proxy support should be opt-in through environment variables or an ignored local override file.

If an existing proxy container is needed later, prefer an explicit external Docker network and document the required network/service names before changing Compose configuration.
