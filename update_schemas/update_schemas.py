import re
import os
import requests
import semantic_version
import time
import shutil
from collections import defaultdict
from git import Repo
from git.exc import GitCommandError


GITHUB_EWP_PATH = "https://github.com/erasmus-without-paper/"
GITHUB_REPOS_PATH = "https://api.github.com/users/erasmus-without-paper/repos?per_page=100"
TEMP_REPOS_DIR = "./temp/"
OUTPUT_DIRECTORY = "../src/main/resources/schemas/"
TEST_FILES_DIR = "../src/test/resources/test-files/latest-examples/"


class GitHubException(Exception):
    pass


def is_next_page_relation(relation):
    return 'rel="next"' in relation


def get_next_page(response):
    raw_links = response.headers.get('link')
    if not raw_links:
        return None

    links = list(map(lambda x: x.split(';'), raw_links.split(',')))
    for link, relation in links:
        if is_next_page_relation(relation):
            return link.strip().strip("<>")

    return None


def download_whole_repository_list():
    result = []
    # repository list is paged, link to the next page is in 'link' header.
    current_page = GITHUB_REPOS_PATH
    while current_page:
        print("Getting", current_page)
        response = requests.get(current_page)
        if response.status_code != 200:
            raise GitHubException("Cannot obtain repositories list.")
        try:
            json = response.json()
            result.extend(json)
        except ValueError:
            raise GitHubException(
                "GitHub response does not contain valid json.")
        current_page = get_next_page(response)
    return result


def get_repositories_list():
    result = []
    repos = download_whole_repository_list()
    for repo in repos:
        if repo['archived']:
            continue

        repo_name = repo['name']
        if select_repo_processor(repo_name) is None:
            print("Omitting", repo_name)
            continue

        repo_url = repo['git_url']
        result.append((repo_name, repo_url))
    return result


def get_tag_version(tag):
    version = tag.name[1:]  # Remove leading 'v'
    return semantic_version.Version(version)


def git_list_files(repo):
    return repo.git.ls_tree("HEAD", name_only=True, r=True, full_tree=True).split('\n')


def get_tag_dict(repos):
    result_dict = {}
    for name, _ in repos:
        print("Collecting tags for", name)
        cloned_dir = TEMP_REPOS_DIR + name
        repo = Repo(cloned_dir)
        tags = list(sorted(map(get_tag_version, repo.tags)))
        repo_dict = defaultdict(list)
        for tag in tags:
            repo_dict[tag.major].append(tag)

        result_dict[name] = dict(repo_dict)

    return dict(result_dict)


class RepoProcessor(object):
    def get_main_file(self):
        return None

    def is_auxilary_file(self, filename):
        return False

    def get_references(self):
        return {}

    def get_allowed_versions(self):
        return None

    def construct_uri_name(self, tree, filename):
        tree_or_blob = "tree" if tree else "blob"
        # This is an edge case scenario, current __index__ file is inconsistent
        if self.version.major == 0 and self.version.minor == 1 and self.version.patch == 0:
            version_marker = "v0.1.0"
        elif self.version.major == 0:
            version_marker = "stable-v1"
        else:
            version_marker = "stable-v" + str(self.version.major)

        file = f"/{filename}" if filename else ""
        return f"{self.uri_name_base}/{tree_or_blob}/{version_marker}{file}"

    def create_copy_entry(self, filename):
        return (
            TEMP_REPOS_DIR + self.name + "/" + filename,
            OUTPUT_DIRECTORY + self.uri_base + filename,
            TEMP_REPOS_DIR + self.name,
            'v' + str(self.version)
        )

    def create_file_entry(self, filename):
        if filename:
            tree = False
            name = filename
        else:
            tree = True
            name = None
            filename = self.get_main_file()

        index_entry = construct_uri_entry(self.construct_uri_name(tree, name),
                                          self.uri_base + filename)
        copy_entry = self.create_copy_entry(filename)
        return index_entry, [copy_entry]

    def create_main_file_entry(self):
        return self.create_file_entry(None)

    def process_auxilary_file(self, filename):
        return self.create_file_entry(filename)

    def __init__(self, name, version, repo):
        self.name = name
        self.version = version
        self.repo = repo

        self.version_string = 'v' + str(version)
        self.files = git_list_files(self.repo)

        self.uri_base = f"{self.name}-v{str(self.version)}/"
        self.uri_name_base = GITHUB_EWP_PATH + self.name

    def __call__(self):
        index_entries = []
        copy_entries = []
        if self.get_allowed_versions() is not None \
                and self.version.major not in self.get_allowed_versions():
            return [], []

        if self.get_main_file() in self.files:
            entries = self.create_main_file_entry()
            index_entries.extend(entries[0])
            copy_entries.extend(entries[1])

        for filename in self.files:
            if filename == self.get_main_file():
                continue

            if self.is_auxilary_file(filename):
                entries = self.process_auxilary_file(filename)
                index_entries.extend(entries[0])
                copy_entries.extend(entries[1])
                continue

            if filename in self.get_references():
                index_entries.extend(construct_uri_entry(self.get_references()[filename], self.uri_base + filename))
                copy_entries.append(self.create_copy_entry(filename))

        return index_entries, copy_entries


class ApiProcessor(RepoProcessor):
    def get_main_file(self):
        return "response.xsd"

    def is_auxilary_file(self, filename):
        return filename == "manifest-entry.xsd"\
            or re.match(".*-response.xsd", filename) is not None\
            or re.match(".*-request.xsd", filename) is not None


class ApiDiscoveryProcessor(RepoProcessor):
    def get_main_file(self):
        return "manifest.xsd"

    def is_auxilary_file(self, filename):
        return filename == "manifest-entry.xsd"

    def get_allowed_versions(self):
        return [4, 5]


class ApiRegistryProcessor(RepoProcessor):
    def get_main_file(self):
        return "catalogue.xsd"

    def is_auxilary_file(self, filename):
        return filename == "manifest-entry.xsd"


class SecurityRepoProcessor(RepoProcessor):
    def get_main_file(self):
        return "security-entries.xsd"


class ArchitectureRepoProcessor(RepoProcessor):
    def get_main_file(self):
        return None

    def is_auxilary_file(self, filename):
        return filename == "common-types.xsd"


class TypesRepoProcessor(RepoProcessor):
    def get_main_file(self):
        return "schema.xsd"


class ApiImobilityTorsProcessor(ApiProcessor):
    def get_references(self):
        return {
            "references/emrex-elmo-v1.1.0/schema.xsd":
                "https://github.com/emrex-eu/elmo-schemas/tree/v1",
            "references/emrex-elmo-v1.1.0/references/EUROPASS_ISOCountries_V1.1.xsd":
                "http://europass.cedefop.europa.eu/Europass/V2.0",
            "references/emrex-elmo-v1.1.0/references/xmldsig-core-schema.xsd":
                "http://www.w3.org/2000/09/xmldsig#"
        }


repository_processors = [
    ("ewp-specs-architecture", ArchitectureRepoProcessor),
    ("ewp-specs-sec-intro", TypesRepoProcessor),
    ("ewp-specs-sec-.*", SecurityRepoProcessor),
    ("ewp-specs-types-.*", TypesRepoProcessor),
    ("ewp-specs-fileext-ewpmobility", TypesRepoProcessor),
    ("ewp-specs-api-emrex-gateway", None),  # It matches one of entries, but should be omitted
    ("ewp-specs-api-mobility-update", None),
    ("ewp-specs-api-discovery", ApiDiscoveryProcessor),
    ("ewp-specs-api-registry", ApiRegistryProcessor),
    ("ewp-specs-api-imobility-tors", ApiImobilityTorsProcessor),
    ("ewp-specs-api-.*", ApiProcessor),
]


def select_repo_processor(name):
    for regex, processor in repository_processors:
        if re.match(regex, name) is not None:
            return processor


def process_repo_version(name, version):
    cloned_dir = TEMP_REPOS_DIR + name
    repo = Repo(cloned_dir)
    repo.git.checkout('v' + str(version))
    processor_factory = select_repo_processor(name)
    if not processor_factory:
        return [], []
    return processor_factory(name, version, repo)()


def create_copy_examples_entries(name, version):
    cloned_dir = TEMP_REPOS_DIR + name
    repo = Repo(cloned_dir)
    repo.git.checkout('v' + str(version))
    processor_factory = select_repo_processor(name)
    if not processor_factory:
        return []
    result = []
    files = git_list_files(repo)
    for filename in files:
        if re.match(".*example.*xml", filename) is not None:
            print("Found example file", cloned_dir + "/" + filename)
            result.append((
                cloned_dir + "/" + filename, f"{TEST_FILES_DIR}{name}-{filename.replace('/', '-')}",
                cloned_dir, 'v' + str(version)
            ))
    return result


def create_index_entries(tags_dict):
    index_entries_result = []
    copy_entries_result = []
    for name, versions_dict in tags_dict.items():
        if len(versions_dict) > 1 and 0 in versions_dict:
            del versions_dict[0]

        for major_version, versions in versions_dict.items():
            print("Creating index for", name, versions[-1])
            index_entries, copy_entries = process_repo_version(name, versions[-1])
            index_entries_result.extend(index_entries)
            copy_entries_result.extend(copy_entries)

        if versions_dict:
            latest_version = versions_dict[max(versions_dict.keys())][-1]
            copy_entries_result.extend(create_copy_examples_entries(name, latest_version))
    return index_entries_result, copy_entries_result


def construct_uri_entry(name, uri):
    indent = '    '
    return ["<uri", indent + f'name="{name}"', indent + f'uri="{uri}" />']


def remove_dir(name):
    try:
        shutil.rmtree(name)
    except FileNotFoundError:
        pass


repos = get_repositories_list()

# Clone all repos
for name, url in repos:
    retries = 0
    max_retries = 3
    retry = True
    while retry:
        print("Cloning ", url, "into", TEMP_REPOS_DIR + name)
        try:
            remove_dir(TEMP_REPOS_DIR + name)
            Repo.clone_from(url, TEMP_REPOS_DIR + name)
            retry = False
        except GitCommandError as e:
            if retries >= max_retries:
                raise
            retries += 1
            print("An error occured, retrying.", e)
            time.sleep(1)


# get tags for each repo
tags_dict = get_tag_dict(repos)

# create index lines and copy commands for each required file
index_entries, copy_entries = create_index_entries(tags_dict)

index_prolog = [
    '<catalog xmlns="urn:oasis:names:tc:entity:xmlns:xml:catalog">',
    *construct_uri_entry("http://www.w3.org/XML/1998/namespace", "xml.xsd"),
]
index_epilog = ['</catalog>']

index_lines = []
index_lines.extend(index_prolog)
index_lines.extend(index_entries)
index_lines.extend(index_epilog)


# Remove old schemas
remove_dir(OUTPUT_DIRECTORY)

# Write index
index_file_name = "__index__.xml"
index_path = OUTPUT_DIRECTORY + index_file_name

os.makedirs(os.path.dirname(index_path), exist_ok=True)
with open(index_path, "w") as f:
    f.write('\n'.join(index_lines))

# Copy schemas

for source, destination, repo_path, version in copy_entries:
    repo = Repo(repo_path)
    repo.git.checkout(version)
    os.makedirs(os.path.dirname(destination), exist_ok=True)
    shutil.copyfile(source, destination)

xml_xsd = "xml.xsd"
source = xml_xsd
destination = OUTPUT_DIRECTORY + xml_xsd
os.makedirs(os.path.dirname(destination), exist_ok=True)
shutil.copyfile(source, destination)

# Cleanup
remove_dir(TEMP_REPOS_DIR)
