# item-handler
Handler service dealing with placed orders and producing individual items to be processed internally

### Requirements
* [Java 21][1]
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
CERTIFIED_COPY_ORDER_CONFIRMATION_RECIPIENT | The recipient certified copy order confirmation emails are sent to. | ✓ | | env var
MISSING_IMAGE_DELIVERY_ORDER_CONFIRMATION_RECIPIENT | The recipient missing image delivery order confirmation emails are sent to. | ✓ | | env var

### Endpoints
Path | Method | Description
--- | --- | ---
*`/healthcheck`* | GET | Returns HTTP OK (`200`) to indicate a healthy application instance.

### Jib Command

```
mvn compile jib:dockerBuild -Dimage=416670754337.dkr.ecr.eu-west-2.amazonaws.com/item-handler:latest
```

[1]: https://www.oracle.com/java/technologies/downloads/#java21
[2]: https://maven.apache.org/download.cgi
[3]: https://git-scm.com/downloads

## Terraform ECS
### What does this code do?
The code present in this repository is used to define and deploy a dockerised container in AWS ECS.
This is done by calling a [module](https://github.com/companieshouse/terraform-modules/tree/main/aws/ecs) from terraform-modules. Application specific attributes are injected and the service is then deployed using Terraform via the CICD platform 'Concourse'.
Application specific attributes | Value                                | Description
:---------|:-----------------------------------------------------------------------------|:-----------
**ECS Cluster**        |order-service                                      | ECS cluster (stack) the service belongs to
**Concourse pipeline**     |[Pipeline link](https://ci-platform.companieshouse.gov.uk/teams/team-development/pipelines/item-handler) <br> [Pipeline code](https://github.com/companieshouse/ci-pipelines/blob/master/pipelines/ssplatform/team-development/item-handler)                               | Concourse pipeline link in shared services
### Contributing
- Please refer to the [ECS Development and Infrastructure Documentation](https://companieshouse.atlassian.net/wiki/spaces/DEVOPS/pages/4390649858/Copy+of+ECS+Development+and+Infrastructure+Documentation+Updated) for detailed information on the infrastructure being deployed.
### Testing
- Ensure the terraform runner local plan executes without issues. For information on terraform runners please see the [Terraform Runner Quickstart guide](https://companieshouse.atlassian.net/wiki/spaces/DEVOPS/pages/1694236886/Terraform+Runner+Quickstart).
- If you encounter any issues or have questions, reach out to the team on the **#platform** slack channel.
### Vault Configuration Updates
- Any secrets required for this service will be stored in Vault. For any updates to the Vault configuration, please consult with the **#platform** team and submit a workflow request.
### Useful Links
- [ECS service config dev repository](https://github.com/companieshouse/ecs-service-configs-dev)
- [ECS service config production repository](https://github.com/companieshouse/ecs-service-configs-production◊