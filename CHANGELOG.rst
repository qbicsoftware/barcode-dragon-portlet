==========
Changelog
==========

This project adheres to `Semantic Versioning <https://semver.org/>`_.

1.5.5
-----

**Added**

**Fixed**

* CVE-2021-44832

**Dependencies**

* ``org.apache.logging.log4j:log4j-api:2.17.0`` -> ``2.17.1``
* ``org.apache.logging.log4j:log4j-core:2.17.0`` -> ``2.17.1``

**Deprecated**

1.5.4
-----

**Added**

**Fixed**

* Some specific sample code types don't lead to nullpointer exceptions any more
* CVE-2021-45105

**Dependencies**

* ``org.apache.logging.log4j:log4j-api:2.16.0`` -> ``2.17.0``
* ``org.apache.logging.log4j:log4j-core:2.16.0`` -> ``2.17.0``

**Deprecated**

1.5.3
-----

**Added**

**Fixed**

* Fix CVE-2021-37714 [Denial of Service Vulnerability](https://vaadin.com/security/2021-10-27)

**Dependencies**

* Increase vaadin-version `7.7.8` -> `7.7.28`
* Increase vaadin-plugin-version `7.7.8` -> `7.7.28`

**Deprecated**

1.5.2
-----

**Added**

**Fixed**

* CVE-2021-45046

**Dependencies**

* ``org.apache.logging.log4j:log4j-core:2.15.0`` -> ``2.16.0``
* ``org.apache.logging.log4j:log4j-api:2.15.0`` -> ``2.16.0``

**Deprecated**


1.5.1
-----

**Added**

**Fixed**

* CVE-2021-44228

**Dependencies**

* ``org.apache.logging.log4j:log4j-core:2.11.0`` -> ``2.15.0``
* ``org.apache.logging.log4j:log4j-api:2.11.0`` -> ``2.15.0``

**Deprecated**


1.5.0
-----

**Added**

* Supports new person table structure

* Supports creation of barcodes for patients and other sample source organisms

* Adds taxonomic name to info selection for source organisms

**Fixed**

**Dependencies**

**Deprecated**

* Old persons table in the database is not supported anymore
