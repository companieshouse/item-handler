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
Name | Description | Mandatory | Default | Location
--- | --- | --- | --- | ---
ITEM_HANDLER_PORT | Port this application runs on when deployed. | ✓ |  | start.sh
IS_ERROR_QUEUE_CONSUMER | Setting to `true` configures app to listen only to `order-received-error`. | ✓ | `false` | env var
CERTIFICATE_ORDER_CONFIRMATION_RECIPIENT | The recipient certificate order confirmation emails are sent to. | ✓ | | env var

### Endpoints
Path | Method | Description
--- | --- | ---
*`/healthcheck`* | GET | Returns HTTP OK (`200`) to indicate a healthy application instance.

[1]: http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
[2]: https://maven.apache.org/download.cgi
[3]: https://git-scm.com/downloads
