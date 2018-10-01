# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/) 
and this project adheres to [Semantic Versioning](http://semver.org/).

## 2.4.0 - 2018-10-01

**IMPORTANT** you will need to fix the mongo.properties file to now
use the uri variable instead of specifying the mongo server.

### Added
- Add docker.sh, release.sh.

### Fixed
- Fixed issue where polyglot could not convert files posted in docker
  [BD-2239](https://opensource.ncsa.illinois.edu/jira/browse/BD-2239)

### Changed
- Use URI to specify mongo connect
  [POL-200](https://opensource.ncsa.illinois.edu/jira/browse/POL-200)
  
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

