# item-handler
Handler service dealing with placed orders and producing individual items to be processed internally

### Requirements
* [Java 8][1]
* [Maven][2]
* [Git][3]

### Getting Started
1. Run `make` to build
2. Run `./start.sh` to run

### Environment Variables
Name | Description | Mandatory | Location
--- | --- | --- | ---
ITEM_HANDLER_PORT | Port this application runs on when deployed. | ✓ | start.sh

### Endpoints
Path | Method | Description
--- | --- | ---
*`/healthcheck`* | GET | Returns HTTP OK (`200`) to indicate a healthy application instance.

[1]: http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
[2]: https://maven.apache.org/download.cgi
[3]: https://git-scm.com/downloads
