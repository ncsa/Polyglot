# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/) 
and this project adheres to [Semantic Versioning](http://semver.org/).

## 0.3.0 - 2018-01-12
### Added
- Added entrypoint option to load env vars.
- Added downloadFile function (with HTTP header User-Agent) in SoftwareServerUtility.java

### Changed
- updates PolyglotRestlet.sh to use correct polyglot-version-SNAPSHOT.jar
- add back old PEcAn file format to compatible with PEcAn. 

### Fixed
- Fixed potential duplicate file overwritten when hosting posted file. [POL-194](https://opensource.ncsa.illinois.edu/jira/browse/POL-194)
- Fixed shell option in entrypoint.

