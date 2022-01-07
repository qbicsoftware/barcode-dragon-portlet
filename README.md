# Barcode Dragon Portlet

Barcode Dragon Portlet, Portlet which allows the creation of sample sheets for customers.


## Description 

![barcode_screenshot_1](https://user-images.githubusercontent.com/21954664/41223131-3a008db8-6d69-11e8-825e-d7a3e9d91d1d.png)

This portlet allows the creation of sample sheets and barcode stickers for customers.
Sample sheets contain QBiC's logo, QBiC's contact information, the customer's name and address and finally the respective QBiC contact's name and contact information.
Additionally, barcodes and detailed information related to the barcodes is added in their respective columns.
Barcodes can be customized, take a look at the screenshots.

![barcode_screenshot_2](https://user-images.githubusercontent.com/21954664/41223134-3bced618-6d69-11e8-9d18-f3a12846e05b.png)

## Requirements

JDK8+

Python 2.7 (Python 3 is not yet supported!)

Ghostscript

Pdflatex

(see also: https://github.com/qbicsoftware/barcode-creation)
 
## How to Install

1. <code>git clone https://github.com/qbicsoftware/barcode-dragon-portlet</code>
4. The folder 'setup_samples' contains an example portlet.properties file. Create a copy and fill out the blank details. Some properties will be filled out in the following steps.
   If you are not aware of the usernames/passwords ask your admin of choice.
5. Download the monolithic release of the postscriptbarcode from https://github.com/bwipp/postscriptbarcode/releases/tag/2017-07-10
6. Place it somewhere and edit the path 'barcode.postscript' in the portlet.properties file.
7. <code> git clone https://github.com/qbicsoftware/barcode-creation</code>
8. Edit the path 'barcode.scripts' in the portlet.properties file: it should now point to the cloned repository in the previous step
9. Navigate into the folder 'wizard_scripts' in the cloned repository
10. Create a file 'properties_path.txt' which solely contains one line: the path to the portlet.properties file of your barcode-dragon-portlet. 
An example is provided in the 'setup_samples' folder of barcode-dragon-portlet
11. Create a file 'test.properties' in 'wizard_scripts' folder. An example is provided in the 'setup_samples' folder.
Fill out the blanks, make sure that they match your configuration in the portlet.properties file of the barcode-dragon-portlet!
12. You have now successfully configured barcode-dragon-portlet!
13. Run the program: <code>mvn jetty:run</code>
http://localhost:8080/ is the default localhost 

## Authors

Created by the QBiC developers.
