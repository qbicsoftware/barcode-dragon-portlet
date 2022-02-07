# Barcode Dragon Portlet

Barcode Dragon Portlet, Portlet which allows the creation of sample sheets for customers.

[![Build Maven Package](https://github.com/qbicsoftware/barcode-dragon-portlet/actions/workflows/build_package.yml/badge.svg)](https://github.com/qbicsoftware/barcode-dragon-portlet/actions/workflows/build_package.yml)
[![Run Maven Tests](https://github.com/qbicsoftware/barcode-dragon-portlet/actions/workflows/run_tests.yml/badge.svg)](https://github.com/qbicsoftware/barcode-dragon-portlet/actions/workflows/run_tests.yml)
[![CodeQL](https://github.com/qbicsoftware/barcode-dragon-portlet/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/qbicsoftware/barcode-dragon-portlet/actions/workflows/codeql-analysis.yml)
[![release](https://img.shields.io/github/v/release/qbicsoftware/barcode-dragon-portlet?include_prereleases)](https://github.com/qbicsoftware/barcode-dragon-portlet/releases)
[![license](https://img.shields.io/github/license/qbicsoftware/barcode-dragon-portlet)](https://github.com/qbicsoftware/barcode-dragon-portlet/blob/main/LICENSE)
![language](https://img.shields.io/badge/language-java-blue.svg)

## How to Run


This portlet requires the following installations on the executing system: 
<ol>
<li><a href="https://www.python.org/download/releases/2.7/">Python 2.7</a></li>
<li><a href="https://www.ghostscript.com/releases/index.html">Ghostscript</a></li>
<li><a href="https://pypi.org/project/pdflatex/0.1.3/">Pdflatex</a></li>
</ol>

Additionally the following repositories have to be downloaded and setup as outlined in the [properties section](#properties): 

<ol>
<li><a href="https://github.com/bwipp/postscriptbarcode/releases/tag/2017-07-10">postscriptbarcode</a></li>
<li><a href="https://github.com/qbicsoftware/barcode-creation">barcode-creation</a></li>
</ol>

Compile the project with java 8 and build a web application archive with Maven:

```
mvn clean package
```

The WAR file will be created in the ``/target`` folder:

```
|-target
|---barcode-dragon-portlet-<VERSION>.war
|---...
```

The application can be run as follows:

```
mvn jetty:run 
```

### Configuration

The default configuration of the app binds to the local port 8080 of the systems localhost:

```
http://localhost:8080
```

#### Properties

To run the tool you need to fill in a multitude of credentials. 
To do so you can copy and adapt the following template portlet.properties found in the ``setup_samples`` directory:

| Property               | Description                                                 | Default Value                   |
|------------------------|-------------------------------------------------------------|---------------------------------|
| `datasource.url`       | The URL to the host of the openBis instance                 | `localhost `                    |
| `datasource.user`      | The openBis user name                                       | `openbisusername`               |
| `datasource.password`  | The openBis user password                                   | `openbispassphrase!`            |
| `tmp.folder`           | Path into which the postscript tmp files should be stored   | `my/postscript/tmp/folder!`     |
| `barcode.postscript`   | Path to the downloaded postscriptbarcode repository         | `/downloads/postscriptbarcode/` |
| `barcode.result`       | Path where the created barcodes should be stored            | `/my/created/barcodes`          |
| `barcode.scripts`      | Path to the downloaded barcodescripts repository            | `downloads/barcodescripts/`     |
| `path.variable`        | Path to the Python 2.7 installation on the executing system | `/my/python/path/`              |
| `mysql.host`           | The URL to the host of the user database                    | `localhost `                    |
| `mysql.db`             | The name of the database                                    | `user_management_database`      |
| `mysql.user`           | The database user name                                      | `dbusername`                    |
| `mysql.port`           | The port on which the user database can be accessed         | `8080`                          |
| `mysql.pass`           | The database user password                                  | `dbpassphrase!`                 |
| `mysql.input.usergrp`  | The liferay user group able to view the barcode printer     | `privilegedliferayuser`         |
| `mysql.input.admingrp` | The liferay admim group able to view the barcode printer    | `privilegedliferayadmin`        |
| `metadata.write.group` | The group able to write metadata                            | `metadata-writers`              |

Additionally you need to provide and adapt the following repositories:

<ol> 
<li><a href="https://github.com/bwipp/postscriptbarcode/releases/tag/2017-07-10">postscriptbarcode</a></li>

Adapt the ``barcode.postscript`` property in the ``portlet.property`` file of the barcode-dragon-portlet repository to the path of the downloaded postscriptbarcode repository

<li><a href="https://github.com/qbicsoftware/barcode-creation" >barcode-creation</a></li> 

First, adapt the ``barcode.scripts`` property in the ``portlet.property`` file of the barcode-dragon-portlet repository to the path of the downloaded barcode-creation repository.

Next, Navigate into the folder ``wizard_scripts`` of the cloned barcode-creation repository and create the following files:

`properties_path.txt` file solely containing the path to the ``portlet.properties`` file of the barcode-dragon-portlet repository: </li>

```/path/to/barcode-dragon-portlet/portlet.properties```

`test.properties` file containing the same property specification as are set in the ``portlet.property`` file of the barcode-dragon-portlet repository:

```
barcode.postscript = <path/to/postscriptbarcodefolder>
barcode.results = <path/to/postscriptbarcodeoutputfolder>
tmp.folder = <path/to/postscriptbarcodetmpfolder>
```

</ol>
An example for both files are provided in the ``setup_samples`` folder of the barcode-dragon-portlet repository

## How to use

![barcode_screenshot_1](https://user-images.githubusercontent.com/21954664/41223131-3a008db8-6d69-11e8-825e-d7a3e9d91d1d.png)

This portlet allows the creation of sample sheets and barcode stickers for customers.
Sample sheets contain QBiC's logo, QBiC's contact information, the customer's name and address and finally the respective QBiC contact's name and contact information.
Additionally, barcodes and detailed information related to the barcodes is added in their respective columns.
Barcodes can be customized, take a look at the screenshots.

![barcode_screenshot_2](https://user-images.githubusercontent.com/21954664/41223134-3bced618-6d69-11e8-9d18-f3a12846e05b.png)