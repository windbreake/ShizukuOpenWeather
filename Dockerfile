FROM ubuntu:24.04

ARG USERNAME=dev
ARG USER_UID=1000
ARG USER_GID=${USER_UID}
ARG NODE_MAJOR=22

ENV DEBIAN_FRONTEND=noninteractive
ENV RUSTUP_HOME=/usr/local/rustup
ENV CARGO_HOME=/usr/local/cargo
ENV SDKMAN_DIR=/usr/local/sdkman
ENV GRADLE_USER_HOME=/home/dev/.gradle
ENV PATH=/usr/local/cargo/bin:/usr/local/sdkman/candidates/gradle/current/bin:/usr/local/sdkman/candidates/kotlin/current/bin:${PATH}

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        ca-certificates \
        curl \
        git \
        gnupg \
        bash-completion \
        build-essential \
        pkg-config \
        libssl-dev \
        sqlite3 \
        libsqlite3-dev \
        openjdk-21-jdk \
        maven \
        unzip \
        zip \
    && mkdir -p /etc/apt/keyrings \
    && curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key | gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg \
    && printf 'deb [signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_%s.x nodistro main\n' "${NODE_MAJOR}" > /etc/apt/sources.list.d/nodesource.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends nodejs \
    && npm install -g npm@latest typescript vue-tsc create-vue \
    && curl -s https://get.sdkman.io | bash \
    && bash -lc 'source /usr/local/sdkman/bin/sdkman-init.sh && sdk install gradle && sdk install kotlin' \
    && curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y --no-modify-path --profile default \
    && rustup component add rustfmt clippy \
    && if getent group "${USER_GID}" >/dev/null; then groupmod --new-name "${USERNAME}" "$(getent group "${USER_GID}" | cut -d: -f1)"; else groupadd --gid "${USER_GID}" "${USERNAME}"; fi \
    && if getent passwd "${USER_UID}" >/dev/null; then usermod --login "${USERNAME}" --home "/home/${USERNAME}" --move-home "$(getent passwd "${USER_UID}" | cut -d: -f1)"; else useradd --uid "${USER_UID}" --gid "${USER_GID}" -m "${USERNAME}" -s /bin/bash; fi \
    && usermod --gid "${USER_GID}" --shell /bin/bash "${USERNAME}" \
    && mkdir -p /workspace \
    && chown -R "${USERNAME}:${USERNAME}" /workspace /usr/local/cargo /usr/local/rustup /usr/local/sdkman \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

USER ${USERNAME}
WORKDIR /workspace

CMD ["bash"]
