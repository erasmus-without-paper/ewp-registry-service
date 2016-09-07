import os
import sys

VARDIR = "/root"


def err(msg):
    print(msg)
    sys.exit(1)


print("Running sanity checks on the mounted Docker volume...")

if len(os.listdir(VARDIR)) == 0:
    err("Container's '" + VARDIR + "' volume seems empty. Did you mount it?")
if not os.path.isfile(VARDIR + "/manifest-sources.xml"):
    err("File 'manifest-sources.xml' is missing.")
if not os.path.isdir(VARDIR + "/repo"):
    err("Missing 'repo' directory. Did you run git init?")
if len(os.listdir(VARDIR + "/repo")) == 0:
    err("The 'repo' directory seems to be empty. Did you run git init?")
if not os.path.isfile(VARDIR + "/application.properties"):
    err("File 'application.properties' is missing.")
