# QBiC Portlet Template

This repository provides a template for a QBiC Liferay Vaadin Portlet based on Maven. 

## Getting Started

To start with the implementation of a QBiC portlet, clone this repository and import it into Eclipse. 

### Creating WAR

The template provides a instantly working demo portlet. Just export the WAR file:
  1. right-click on the project
  2. Export --> WAR file

### Deployment

To deploy the portlet move the WAR file in the liferay/deploy folder of your Liferay instance.

## Branch overview:

* developer: used during the development
  * feature: used for each feature
* testing: auto-deploys on testing (Jenkins)
* release: handles the releases
* production: auto-deploys on production (Jenkins)
