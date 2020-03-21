#!/bin/bash

set -eu

if [[ "$#" -ne 1 ]]; then
  echo "Usage: $0 <Address of repository to clone using SSH>"
  exit 1
fi


GIT_SSH_REPO_REGEX="^git@.*\.git$"
if [[ ! "$1" =~ $GIT_SSH_REPO_REGEX ]]; then
  echo 'First parameter should be SSH git repository URL. Expected format: "git@*.git"'
  exit 1
fi

git clone "$1" /root/repo
