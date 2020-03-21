FROM openjdk:8-jre

RUN \
    apt-get update && \
    apt-get install -y \
        git \
        python3 \
    && \
    rm -rf /var/lib/apt/lists/*

COPY ssh_config /etc/ssh/ssh_config

COPY start-wrapper.sh /start-wrapper.sh
COPY validate.py /validate.py
COPY ewp-registry*.jar /app.jar
COPY github_repo_scripts/clone_github_repository.sh /clone_github_repository.sh
COPY github_repo_scripts/verify_if_repository_is_writeable.sh /verify_if_repository_is_writeable.sh

VOLUME /root
WORKDIR /root
ENTRYPOINT ["/bin/bash", "/start-wrapper.sh"]
EXPOSE 8080
