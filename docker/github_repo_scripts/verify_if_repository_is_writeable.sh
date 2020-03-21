#!/bin/bash
set -eu

ssh -T git@github.com 2>&1 | grep -q "successfully authenticated"
cd repo
INSTANCE_NAME=$(cat ../application.properties | grep app.instance-name | cut -f2 -d"=")
BRANCH_NAME_UNSAFE="test-run-check-$(date -Is)-$INSTANCE_NAME"
BRANCH_NAME=$(echo $BRANCH_NAME_UNSAFE | sed -r 's/[^a-zA-Z0-9]+/-/g' | sed -r 's/^-+\|-+$//g' | tr A-Z a-z)

git config user.email "test@example.invalid"
git config user.name "test-$BRANCH_NAME"

git checkout master
git pull
git checkout -b $BRANCH_NAME
git push --set-upstream origin $BRANCH_NAME
TEST_FILE_NAME="test-file-for-branch-$BRANCH_NAME"
touch $TEST_FILE_NAME
echo "$BRANCH_NAME" > $TEST_FILE_NAME
git add $TEST_FILE_NAME
git commit -m"Test commit $BRANCH_NAME"
git push
git checkout master
git branch -D $BRANCH_NAME
echo "Verified - OK."
