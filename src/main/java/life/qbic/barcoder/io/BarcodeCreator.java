package life.qbic.barcoder.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;

import life.qbic.barcoder.control.BarcodeController;
import life.qbic.barcoder.helpers.Styles;
import life.qbic.barcoder.logging.Logger;
import life.qbic.barcoder.model.FileType;
import life.qbic.barcoder.model.IBarcodeBean;
import life.qbic.portal.liferayandvaadinhelpers.main.LiferayAndVaadinUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.vaadin.server.FileResource;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.UI;

import life.qbic.barcoder.logging.Log4j2Logger;
import life.qbic.barcoder.model.Person;
import life.qbic.barcoder.processes.IReadyRunnable;
import life.qbic.barcoder.processes.ProcessBuilderWrapper;
import life.qbic.barcoder.processes.UpdateProgressBar;

/**
 * Provides methods to create barcode files for openBIS samples as well as a sample sheet.
 *
 * @author Andreas Friedrich
 */
public class BarcodeCreator {

    private BarcodeConfig config;
    Logger logger = new Log4j2Logger(BarcodeCreator.class);
    private String currentPrintDirectory;

    /**
     * Create a new BarcodeCreator
     *
     * @param config
     */
    public BarcodeCreator(BarcodeConfig config) {
        this.config = config;
    }

    /**
     * Checks if a barcode exists in the barcodes folder for the given sample ID and file type. There
     * are PDF barcodes (for tubes) and PNG barcodes for the sample sheet. This function is
     * automatically used before creating a barcode, but can be used to provide additional information
     * to the user.
     *
     * @param filename Sample ID of the sample the searched barcode belongs to
     * @param type     File type of the barcode
     * @return True, if the barcode already exists on the server, false otherwise
     */
    public boolean barcodeExists(String filename, FileType type) {
        String project = filename.substring(0, 5);
        String t = type.toString().toLowerCase();
        String base = config.getResultsFolder() + "/" + project + "/";
        return new File(base + t + "/" + filename + "." + t).isFile();
    }

    /**
     * Creates barcodes that are not found in a background thread and shows Progress. Afterwards, a
     * provided runnable is executed.
     *
     * @param samps List of BarcodeBeans of the sample barcodes that should be created.
     * @param bar   A ProgressBar element that will show progress while barcodes are created.
     * @param info  A Label element that will show progress info while barcodes are created.
     * @param ready Instance of a class that implements the Runnable interface and is executed when
     *              all barcodes are ready.
     */
    public void findOrCreateSheetBarcodesWithProgress(List<IBarcodeBean> samps, final ProgressBar bar,
                                                      final Label info, final Runnable ready) {
        final List<IBarcodeBean> missingForSheet = new ArrayList<IBarcodeBean>();
        for (int i = 0; i < samps.size(); i++) {
            IBarcodeBean s = samps.get(i);
            if (!barcodeExists(s.getCode(), FileType.PNG))
                missingForSheet.add(s);
        }
        // for progress bar
        final int todo = missingForSheet.size();
        if (todo > 0) {
            Thread t = new Thread(new Runnable() {
                volatile int current = 0;

                @Override
                public void run() {
                    if (missingForSheet.size() > 0) {
                        for (int i = 0; i < missingForSheet.size(); i++) {
                            current++;
                            double frac = current * 1.0 / todo;
                            UI.getCurrent().access(new UpdateProgressBar(bar, info, frac));

                            List<String> cmd = new ArrayList<String>();
                            cmd.add("python");
                            cmd.add(config.getScriptsFolder() + "sheet_barcodes.py");
                            IBarcodeBean b = missingForSheet.get(i);
                            cmd.add(b.getCode());
                            ProcessBuilderWrapper pbd = null;
                            try {
                                pbd = new ProcessBuilderWrapper(cmd, config.getPathVar());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (pbd.getStatus() != 0) {
                                logger.error("Command has terminated with status: " + pbd.getStatus());
                                logger.error("Error: " + pbd.getErrors());
                                logger.error("Last command sent: " + cmd);
                            }
                        }
                    }
                    // go to next page
                    UI.getCurrent().access(ready);
                    UI.getCurrent().setPollInterval(-1);
                }
            });
            t.start();
            UI.getCurrent().setPollInterval(200);
        } else {
            UI.getCurrent().access(ready);
        }
    }

    /**
     * Creates a string with leading zeroes from a number
     *
     * @param id     number
     * @param length of the final string
     * @return the completed String with leading zeroes
     */
    private String createCountString(int id, int length) {
        String res = Integer.toString(id);
        while (res.length() < length) {
            res = "0" + res;
        }
        return res;
    }

    /**
     * Creates barcodes that are not found in a background thread and shows Progress. Afterwards, a
     * provided runnable is executed. Barcode names are prefixed to denote their order for the
     * printing.
     *
     * @param samps List of BarcodeBeans of the sample barcodes that should be created.
     * @param bar   A ProgressBar element that will show progress while barcodes are created.
     * @param info  A Label element that will show progress info while barcodes are created.
     * @param ready Instance of a class that implements the Runnable interface and is executed when
     *              all barcodes are ready.
     */
    public void findOrCreateTubeBarcodesWithProgress(List<IBarcodeBean> samps, final ProgressBar bar,
                                                     final Label info, final Runnable ready) {
        final List<IBarcodeBean> missingForTube = new ArrayList<IBarcodeBean>();

        final String projectPath = config.getResultsFolder() + samps.get(0).getCode().substring(0, 5);
        Date date = new java.util.Date();
        String timeStamp =
                new Timestamp(date.getTime()).toString().split(" ")[1].replace(":", "").replace(".", "");
        final File printDirectory = new File(projectPath + "/" + timeStamp);
        currentPrintDirectory = printDirectory.toString();

        for (int i = 0; i < samps.size(); i++) {
            IBarcodeBean s = samps.get(i);
            // if (!barcodeExists(prefix + s.getCode(), FileType.PDF) || overwrite)
            missingForTube.add(s);
        }
        // for progress bar
        final int todo = missingForTube.size();
        if (todo > 0) {
            Thread t = new Thread(new Runnable() {
                volatile int current = 0;

                @Override
                public void run() {
                    if (missingForTube.size() > 0) {
                        for (int i = 0; i < missingForTube.size(); i++) {
                            current++;
                            double frac = current * 1.0 / todo;
                            UI.getCurrent().access(new UpdateProgressBar(bar, info, frac));

                            List<String> cmd = new ArrayList<String>();
                            cmd.add("python");
                            cmd.add(config.getScriptsFolder() + "tube_barcodes.py");
                            IBarcodeBean b = missingForTube.get(i);
                            String prefix = createCountString(i + 1, 4) + "_";// used for ordered printing
                            cmd.add(prefix + b.getCode());
                            cmd.add(b.getCodedString());
                            if (b.firstInfo() == null)
                                cmd.add(" ");
                            else
                                cmd.add(b.firstInfo());
                            if (b.altInfo() == null)
                                cmd.add(" ");
                            else
                                cmd.add(b.altInfo());
                            ProcessBuilderWrapper pbd = null;
                            try {
                                pbd = new ProcessBuilderWrapper(cmd, config.getPathVar());

                                String file = prefix + missingForTube.get(i).getCode() + ".pdf";
                                File cur = new File(projectPath + "/pdf/" + file);
                                File dest = new File(printDirectory.toString() + "/" + file);
                                try {
                                    if (!printDirectory.exists())
                                        printDirectory.mkdir();
                                    Files.copy(cur.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (pbd.getStatus() != 0) {
                                logger.error("Command has terminated with status: " + pbd.getStatus());
                                logger.error("Error: " + pbd.getErrors());
                                logger.error("Last command sent: " + cmd);
                            }
                        }
                    }
                    // go to next page
                    UI.getCurrent().access(ready);
                    UI.getCurrent().setPollInterval(-1);
                }
            });
            t.start();
            UI.getCurrent().setPollInterval(200);
        } else {
            UI.getCurrent().access(ready);
        }
    }

    public int getNumberOfAvailableBarcodes() {
        int n = 0;
        try {
            n = new File(currentPrintDirectory).listFiles().length;
        } catch (NullPointerException e) {
            // fails if folder doesn't exist ==> 0 tubes available
        }
        return n;
    }

    /**
     * Creates a sample sheet given a list of ISampleBeans and the preferred sorting. Barcode files
     * must already exist for all samples!
     *
     * @param ids      List of ISampleBeans whose barcodes should be part of the sample sheet.
     * @param colNames The two chosen column headers in form of a list
     * @return
     */
    public FileResource createAndDLSheet(String projectCode, String projectName, Person investigator,
                                         Person contact, List<IBarcodeBean> samps, List<String> colNames) {
        String jsonParamPath = null;
        try {
            jsonParamPath = writeJSON(config.getTmpFolder() + createTimeStamp() + ".json",
                    sheetInfoToJSON(projectCode, projectName, investigator, contact, samps, colNames));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        final Calendar calendar = Calendar.getInstance();
        String[] time = calendar.getTime().toString().split(" ");
        String date = time[5] + time[1] + time[2];

        String project = samps.get(0).getCode().substring(0, 5);

        List<String> cmd = new ArrayList<String>();
        cmd.add("python");
        cmd.add(config.getScriptsFolder() + "samp_sheet.py");
        cmd.add(jsonParamPath);
        // cmd.add(colNames.get(0));
        // cmd.add(colNames.get(1));
        // for (IBarcodeBean b : samps) {
        // cmd.add(b.getCode());
        // cmd.add(b.firstInfo());
        // cmd.add(b.altInfo());
        // }
        ProcessBuilderWrapper pbd = null;
        try {
            pbd = new ProcessBuilderWrapper(cmd, config.getPathVar());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (pbd.getStatus() != 0) {
            logger.error("Sheet creation - command has terminated with status: " + pbd.getStatus());
            logger.error("Error: " + pbd.getErrors());
            logger.error("Last command sent: " + cmd);
        }
        String dlPath = config.getResultsFolder() + project + "/documents/sample_sheets/sample_sheet_"
                + project + "_" + date + ".doc";
        FileResource resource = new FileResource(new File(dlPath));
        new File(jsonParamPath).delete();
        return resource;
    }

    /**
     * Zips the tube barcode files of a list of samples and opens a download link in the browser. All
     * files must exist for this!
     *
     * @param samps List of IBarcodeBean samples whose barcode files will be zipped and downloaded.
     * @return
     */
    public FileResource zipAndDownloadBarcodes(List<IBarcodeBean> samps) {
        List<String> cmd = new ArrayList<String>();

        // programs and paths
        String project = samps.get(0).getCode().substring(0, 5);
        String barcodePath = config.getResultsFolder() + project + "/pdf/";
        final String dlPath = barcodePath + project + "_barcodes.zip";
        new File(dlPath).delete();
        cmd.add("zip");
        cmd.add("-j");
        cmd.add(dlPath);

        for (int i = 0; i < samps.size(); i++) {
            String id = samps.get(i).getCode();
            String prefix = createCountString(i + 1, 4) + "_";// used for ordered printing
            cmd.add(barcodePath + prefix + id + ".pdf");
        }
        // zip
        ProcessBuilderWrapper pbd = null;
        try {
            pbd = new ProcessBuilderWrapper(cmd, config.getPathVar());
        } catch (Exception e) {
            e.printStackTrace();
        }
        // download
        FileResource res = new FileResource(new File(dlPath));
        if (pbd.getStatus() != 0) {
            logger.error("Zipping barcodes - command has terminated with status: " + pbd.getStatus());
            logger.error("Error: " + pbd.getErrors());
            logger.error("Last command sent: " + cmd);
        }
        return res;
    }

    /**
     * Given the 5 letter project code uses a script to print the available tube barcodes on the
     * barcode printer
     *
     * @param projectName
     */
    public void printBarcodeFolderForProject(String projectName, final String hostname,
                                             final String printerName, final String printerLocation, final String space, final IReadyRunnable ready, final BarcodeController controller) {

        final Thread t = new Thread(new Runnable() {

            public void run() {

                final String pdfWildcard = "*.pdf";

                Path printDir = Paths.get(currentPrintDirectory);

                final String pathToBarcodesWithWildcard = String.format("%s%s%s",
                        printDir.toAbsolutePath().toString(),
                        File.separator,
                        pdfWildcard);

                List<String> cmd = new ArrayList<>();

                // lpr -H [ip/host name] -P [printer name] [file] <-- see our wiki
                cmd.add("bash");
                cmd.add("-c");
                cmd.add(String.format("lpr -H %s -P %s %s ", hostname, printerName, pathToBarcodesWithWildcard));

                ProcessBuilderWrapper pbd = null;
                try {
                    logger.debug("sending command: " + cmd);
                    pbd = new ProcessBuilderWrapper(cmd, config.getPathVar());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (pbd.getStatus() != 0) {
                    logger.error(
                            "Printing barcodes - command has terminated with status: " + pbd.getStatus());
                    logger.error("Error: " + pbd.getErrors());
                    logger.error("Last command sent: " + cmd);
                    UI.getCurrent().access(ready);
                    UI.getCurrent().setPollInterval(-1);
                    ready.setSuccess(false);
                    try {

                        controller.getDbManager().addLabelCountEntry(printerName, printerLocation, space, LiferayAndVaadinUtils.getUser().getScreenName(), projectName, getNumberOfAvailableBarcodes());
                    }catch(Exception e){
                        StringBuilder sb = new StringBuilder();
                        for(int i = 0; i < e.getStackTrace().length; i++){
                            sb.append(e.getStackTrace()[i]);
                        }
                        Styles.notification("Printing error", sb.toString(), Styles.NotificationType.ERROR);
                        controller.getDbManager().addLabelCountEntry(printerName, printerLocation, space, sb.toString(), projectName, getNumberOfAvailableBarcodes());


                    }
                    return;
                }

                // Finished
                ready.setSuccess(true);
                try {
                    controller.getDbManager().addLabelCountEntry(printerName, printerLocation, space, LiferayAndVaadinUtils.getUser().getFullName(), projectName, getNumberOfAvailableBarcodes());
                }catch(Exception e){
                    StringBuilder sb = new StringBuilder();
                    for(int i = 0; i < e.getStackTrace().length; i++){
                        sb.append(e.getStackTrace()[i]);
                    }
                    Styles.notification("Printing error", sb.toString(), Styles.NotificationType.ERROR);
                    controller.getDbManager().addLabelCountEntry(printerName, printerLocation, space, sb.toString(), projectName, getNumberOfAvailableBarcodes());


                }
                UI.getCurrent().access(ready);
                UI.getCurrent().setPollInterval(-1);
            }
        });
        t.start();
        UI.getCurrent().setPollInterval(200);
    }

    // TODO
    // public String tubeInfoToJSON() {
    //
    // }

    public String sheetInfoToJSON(String projectCode, String projectName, Person investigator,
                                  Person contact, List<IBarcodeBean> samps, List<String> colNames) throws IOException {
        JSONObject obj = new JSONObject();
        obj.put("project_code", new String(projectCode));
        obj.put("project_name", new String(projectName));

        JSONObject inv = new JSONObject();
        if (investigator != null) {
            inv.put("first", investigator.getFirstName());
            inv.put("last", investigator.getLastName());
            inv.put("phone", investigator.getPhone());
        }
        obj.put("investigator", inv);

        JSONObject cont = new JSONObject();
        if (contact != null) {
            cont.put("first", contact.getFirstName());
            cont.put("last", contact.getLastName());
            cont.put("phone", contact.getPhone());
        }
        obj.put("contact", cont);

        obj.put("cols", colNames);

        JSONArray samples = new JSONArray();
        for (IBarcodeBean b : samps) {
            JSONObject s = new JSONObject();
            s.put("code", b.getCode());
            s.put("info", b.firstInfo());
            s.put("alt_info", b.altInfo());
            samples.add(s);
        }
        obj.put("samples", samples);

        StringWriter out = new StringWriter();
        obj.writeJSONString(out);
        return out.toString();
    }

    private String createTimeStamp() {
        Date date = new java.util.Date();
        return new Timestamp(date.getTime()).toString().split(" ")[1].replace(":", "").replace(".", "");
    }

    private String writeJSON(String path, String object) throws IOException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), "utf-8"));
        writer.write(object);
        writer.close();
        return path;
    }
//
//  public void testTubeBarcodeCreation(List<IBarcodeBean> samps, final TestReadyRunnable ready) {
//    final List<IBarcodeBean> missingForTube = new ArrayList<IBarcodeBean>();
//
//    final String projectPath = config.getResultsFolder() + samps.get(0).getCode().substring(1, 5);
//    String timeStamp = createTimeStamp();
//    final File printDirectory = new File(projectPath + "/" + timeStamp);
//    currentPrintDirectory = printDirectory.toString();
//
//    for (int i = 0; i < samps.size(); i++) {
//      IBarcodeBean s = samps.get(i);
//      // if (!barcodeExists(prefix + s.getCode(), FileType.PDF) || overwrite)
//      missingForTube.add(s);
//    }
//    // for progress bar
//    final int todo = missingForTube.size();
//    if (todo > 0) {
//      if (missingForTube.size() > 0) {
//        for (int i = 0; i < missingForTube.size(); i++) {
//
//          List<String> cmd = new ArrayList<String>();
//          cmd.add("python");
//          cmd.add(config.getScriptsFolder() + "tube_barcodes.py");
//          IBarcodeBean b = missingForTube.get(i);
//          String prefix = createCountString(i + 1, 4) + "_";// used for ordered printing
//          cmd.add(prefix + b.getCode());
//          cmd.add(b.getCodedString());
//          if (b.firstInfo() == null)
//            cmd.add(" ");
//          else
//            cmd.add(b.firstInfo());
//          if (b.altInfo() == null)
//            cmd.add(" ");
//          else
//            cmd.add(b.altInfo());
//          ProcessBuilderWrapper pbd = null;
//          try {
//            pbd = new ProcessBuilderWrapper(cmd, config.getPathVar());
//            System.out.println(pbd.getStatus());
//            System.out.println(pbd.getErrors());
//
//            if (pbd.getStatus() != 0) {
//              logger.error("Command has terminated with status: " + pbd.getStatus());
//              logger.error("Error: " + pbd.getErrors());
//              logger.error("Last command sent: " + cmd);
//            }
//
//            String file = prefix + missingForTube.get(i).getCode() + ".pdf";
//            File cur = new File(projectPath + "/pdf/" + file);
//            File dest = new File(printDirectory.toString() + "/" + file);
//            try {
//              if (!printDirectory.exists())
//                printDirectory.mkdir();
//              Files.copy(cur.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
//            } catch (IOException e) {
//              // TODO Auto-generated catch block
//              e.printStackTrace();
//            }
//          } catch (Exception e) {
//            e.printStackTrace();
//          }
//        }
//      }
//    }
//  }

}
