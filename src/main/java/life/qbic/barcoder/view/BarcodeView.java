/*******************************************************************************
 * QBiC Project Wizard enables users to create hierarchical experiments including different study
 * conditions using factorial design. Copyright (C) "2016" Andreas Friedrich
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package life.qbic.barcoder.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.shared.Position;
import com.vaadin.ui.*;
import life.qbic.barcoder.control.SampleFilterDecorator;
import life.qbic.barcoder.control.SampleFilterGenerator;
import life.qbic.barcoder.helpers.Styles;
import life.qbic.barcoder.logging.Log4j2Logger;
import life.qbic.barcoder.logging.Logger;
import org.tepi.filtertable.FilterTable;

import life.qbic.barcoder.model.ExperimentBarcodeSummary;
import life.qbic.barcoder.model.Printer;
import life.qbic.barcoder.model.Printer.PrinterType;
import life.qbic.barcoder.model.SortBy;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Sample;

import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.ui.themes.ValoTheme;
import life.qbic.barcoder.control.BarcodeController;
import life.qbic.barcoder.helpers.SampleToBarcodeFieldTranslator;

/**
 * View class for the Sample Sheet and Barcode pdf creation
 *
 * @author Andreas Friedrich
 */
public class BarcodeView extends HorizontalLayout {

  Logger logger = new Log4j2Logger(BarcodeView.class);
  /**
   *
   */
  private static final long serialVersionUID = 5688919972212199869L;
  private ComboBox spaceBox;
  private ComboBox projectBox;
  private Table experimentTable;
  private OptionGroup sortby;
  private Map<Object, ExperimentBarcodeSummary> experiments;
  private Map<Object, Sample> samples;

  private Component tabsTab;
  private TabSheet tabs;

  private BarcodePreviewComponent tubePreview;

  private SheetOptionComponent sheetPreview;
  private Button prepareBarcodes;
  private ComboBox printerSelection;
  private Button printTubeCodes;

  private FilterTable sampleTable;

  private ProgressBar bar;
  private Label info;
  private Button download;
  private Map<String, Printer> printerMap;
  private boolean isAdmin;

  private List<String> barcodeSamples = new ArrayList<String>(Arrays.asList("Q_BIOLOGICAL_SAMPLE",
      "Q_TEST_SAMPLE", "Q_NGS_SINGLE_SAMPLE_RUN", "Q_MHC_LIGAND_EXTRACT"));

  /**
   * Creates a new component view for barcode creation
   *
   * @param spaces List of available openBIS spaces
   * @param isAdmin
   * @param gen
   */
  public BarcodeView(List<String> spaces, boolean isAdmin, SampleFilterGenerator gen) {
    VerticalLayout left = new VerticalLayout();
    VerticalLayout right = new VerticalLayout();
    initSampleTable(gen);
    right.addComponent(sampleTable);

    left.setSpacing(true);
    left.setMargin(true);
    right.setSpacing(true);
    right.setMargin(true);

    SampleToBarcodeFieldTranslator translator = new SampleToBarcodeFieldTranslator();
    this.isAdmin = isAdmin;

    spaceBox = new ComboBox("Project", spaces);
    spaceBox.setStyleName(Styles.boxTheme);
    spaceBox.setFilteringMode(FilteringMode.CONTAINS);
    spaceBox.setNullSelectionAllowed(false);
    spaceBox.setImmediate(true);

    projectBox = new ComboBox("Sub-Project");
    projectBox.setStyleName(Styles.boxTheme);
    projectBox.setEnabled(false);
    projectBox.setImmediate(true);
    projectBox.setNullSelectionAllowed(false);

    left.addComponent(Styles.questionize(spaceBox, "Name of the project", "Project Name"));
    left.addComponent(
        Styles.questionize(projectBox, "QBiC project code and project name", "Sub-Project"));

    initExperimentTable();
    left.addComponent(Styles.questionize(experimentTable,
        "This table gives an overview of tissue samples and extracted materials"
            + " for which barcodes can be printed. You can select one or multiple rows.",
        "Sample Overview"));

    sortby = new OptionGroup("Sort Barcodes By");
    sortby.addItems(SortBy.values());
    sortby.setValue(SortBy.BARCODE_ID);
    left.addComponent(sortby);

    sheetPreview = new SheetOptionComponent(translator);
    tubePreview = new BarcodePreviewComponent(translator);

    tabs = new TabSheet();
    tabs.setStyleName(ValoTheme.TABSHEET_FRAMED);
    tabs.addTab(sheetPreview, "Sample Sheet");
    tabs.addTab(tubePreview, "Barcode Stickers");
    tabsTab = new CustomVisibilityComponent(tabs);
    tabsTab.setVisible(false);
    left.addComponent(Styles.questionize(tabsTab,
        "Prepare an A4 sample sheet or barcodes for sample tubes.", "Barcode Preparation"));

    info = new Label();
    bar = new ProgressBar();
    bar.setVisible(false);
    left.addComponent(info);
    left.addComponent(bar);

    prepareBarcodes = new Button("Prepare Barcodes");
    prepareBarcodes.setEnabled(false);
    left.addComponent(prepareBarcodes);

    printerSelection = new ComboBox("Printer");
    printerSelection.setWidth("300px");
    printerSelection.setStyleName(Styles.boxTheme);
    printerSelection.setVisible(false);
    printerSelection.setNullSelectionAllowed(false);
    left.addComponent(printerSelection);

    printTubeCodes = new Button("Print Barcodes");
    printTubeCodes.setVisible(isAdmin);
    printTubeCodes.setEnabled(false);
    left.addComponent(printTubeCodes);

    download = new Button("Download");
    download.setEnabled(false);
    left.addComponent(download);
    addComponent(left);
    addComponent(right);
    disablePreview();
  }

  private void initExperimentTable() {
    experimentTable = new Table("Experiments");
    experimentTable.setStyleName(ValoTheme.TABLE_SMALL);
    experimentTable.setPageLength(1);
    experimentTable.setSelectable(true);
    experimentTable.setMultiSelect(true);
    experimentTable.addContainerProperty("Samples", String.class, null);
    experimentTable.addContainerProperty("Type", String.class, null);
    experimentTable.addContainerProperty("Date", String.class, null);
    experimentTable.addContainerProperty("Experiment", String.class, null);
  }

  private void initSampleTable(SampleFilterGenerator gen) {
    sampleTable = new FilterTable("Samples (optional sub-selection)");
    sampleTable.setStyleName(ValoTheme.TABLE_SMALL);
    sampleTable.setPageLength(1);
    sampleTable.setSelectable(true);
    sampleTable.setMultiSelect(true);
    sampleTable.addContainerProperty("QBiC Code", String.class, null);
    sampleTable.addContainerProperty("Secondary Name", String.class, null);
    sampleTable.addContainerProperty("Lab ID", String.class, null);
    sampleTable.addContainerProperty("Type", String.class, null);
    sampleTable.setColumnWidth("Lab ID", 120);
    sampleTable.setColumnWidth("Type", 130);
    sampleTable.setFilterDecorator(new SampleFilterDecorator());
    sampleTable.setFilterGenerator(gen);
    sampleTable.setFilterBarVisible(true);
    sampleTable.setImmediate(true);
    sampleTable.setVisible(false);
  }

  public void creationPressed() {
    experimentTable.setEnabled(false);
    sampleTable.setEnabled(false);
    spaceBox.setEnabled(false);
    projectBox.setEnabled(false);
    prepareBarcodes.setEnabled(false);
  }

  public void resetOptions() {
    info.setValue("");
    download.setEnabled(false);
    spaceBox.setEnabled(true);
    projectBox.setEnabled(true);
  }

  public void resetProjects() {
    projectBox.removeAllItems();
    projectBox.setEnabled(false);
    resetExperiments();
  }

  public void resetExperiments() {
    experimentTable.setPageLength(1);
    experimentTable.removeAllItems();
    tabsTab.setVisible(false);
    resetSamples();
  }

  public void resetSamples() {
    sampleTable.setPageLength(1);
    sampleTable.removeAllItems();
    sampleTable.setVisible(false);
  }

  public String getSpaceCode() {
    return (String) spaceBox.getValue();
  }

  public String getProjectCode() {
    String res = (String) projectBox.getValue();
    if (res != null && res.contains(" "))
      res = res.split(" ")[0];
    return res;
  }

  public ComboBox getSpaceBox() {
    return spaceBox;
  }

  public ComboBox getProjectBox() {
    return projectBox;
  }

  public Table getExperimentTable() {
    return experimentTable;
  }

  public void setProjectCodes(List<String> projects) {
    projectBox.addItems(projects);
    projectBox.setEnabled(true);
  }

  public void setExperiments(Collection<ExperimentBarcodeSummary> collection) {
    experiments = new HashMap<Object, ExperimentBarcodeSummary>();
    int i = 0;
    for (ExperimentBarcodeSummary s : collection) {
      i++;
      List<Object> row = new ArrayList<Object>();
      row.add(s.getAmount());
      row.add(s.getBio_Type());
      row.add(s.getDate());
      row.add(s.getExperiment());
      experiments.put(i, s);
      experimentTable.addItem(row.toArray(new Object[row.size()]), i);
    }
    experimentTable.setPageLength(collection.size());
  }

  public void setSamples(List<Sample> sampleList, Map<Sample, String> types) {
    samples = new HashMap<Object, Sample>();
    int i = 0;
    for (Sample s : sampleList) {
      i++;
      List<Object> row = new ArrayList<Object>();
      Map<String, String> props = s.getProperties();
      row.add(s.getCode());
      row.add(props.get("Q_SECONDARY_NAME"));
      row.add(props.get("Q_EXTERNALDB_ID"));
      row.add(getType(s, props, types));
      samples.put(i, s);
      sampleTable.addItem(row.toArray(new Object[row.size()]), i);
      sampleTable.select(i);
    }
    sampleTable.setPageLength(sampleList.size());
    sampleTable.setVisible(!sampleList.isEmpty());
  }

  private String getType(Sample s, Map<String, String> props, Map<Sample, String> types) {
    String type = s.getSampleTypeCode();
    String bioType = null;
    if (type.equals(barcodeSamples.get(0))) {
      String tissue = props.get("Q_PRIMARY_TISSUE");
      String detailedTissue = props.get("Q_TISSUE_DETAILED");
      if (detailedTissue == null || detailedTissue.isEmpty())
        bioType = tissue;
      else
        bioType = detailedTissue;
    } else if (type.equals(barcodeSamples.get(1)))
      bioType = s.getProperties().get("Q_SAMPLE_TYPE");
    else if (type.equals(barcodeSamples.get(2)))
      bioType = types.get(s);
    else if (type.equals(barcodeSamples.get(3)))
      bioType = props.get("Q_MHC_CLASS");
    return bioType;
  }

  @SuppressWarnings("unchecked")
  public Collection<ExperimentBarcodeSummary> getSelectedExperiments() {
    List<ExperimentBarcodeSummary> res = new ArrayList<ExperimentBarcodeSummary>();
    for (Object id : (Collection<Object>) experimentTable.getValue()) {
      res.add(experiments.get(id));
    }
    return res;
  }

  public List<Sample> getSelectedSamples() {
    List<Sample> res = new ArrayList<Sample>();
    for (Object id : (Collection<Object>) sampleTable.getValue()) {
      if (sampleTable.containsId(id)) {
        res.add(samples.get(id));
      }
    }
    return res;
  }

  public List<Button> getButtons() {
    return new ArrayList<Button>(Arrays.asList(this.prepareBarcodes, this.printTubeCodes));
  }

  public ProgressBar getProgressBar() {
    return bar;
  }

  public Label getProgressInfo() {
    return info;
  }

  public void enablePrep(boolean enable) {
    setAvailableTubes(0);
    prepareBarcodes.setEnabled(enable);
    tabsTab.setVisible(enable);
  }

  public SortBy getSorter() {
    return (SortBy) sortby.getValue();
  }

  public void creationDone() {
    experimentTable.setEnabled(true);
    sampleTable.setEnabled(true);
    bar.setVisible(false);
  }

  public void tubeCreationDone(int tubeCodes) {
    creationDone();
    setAvailableTubes(tubeCodes);
  }

  public void setAvailableTubes(int n) {
    printTubeCodes.setEnabled(n > 0);
    printTubeCodes.setCaption("Print Barcodes (" + n + ")");
  }

  public void sheetReady() {
    download.setEnabled(true);
  }

  public void tubesReady() {
    download.setEnabled(true);
  }

  public void resetSpace() {
    spaceBox.setValue(null);
  }

  public void disablePreview() {
    tubePreview.setVisible(false);
    printerSelection.setVisible(false);
    printTubeCodes.setVisible(false);
  }

  public void enableTubeLabelPreview(Sample sample) {
    tubePreview.setExample(sample);
    tubePreview.setVisible(true);
    boolean printAvailable = printerSelection.size() > 0;
    printerSelection.setVisible(printAvailable);
    printTubeCodes.setVisible(printAvailable);
  }

  public String getCodedString(Sample s) {
    if (tabs.getSelectedTab() instanceof BarcodePreviewComponent)
      return tubePreview.getCodeString(s);
    else
      return s.getCode();
  }

  public String getInfo1(Sample s, String parents) {
    if (tabs.getSelectedTab() instanceof BarcodePreviewComponent)
      return tubePreview.getInfo1(s);
    else
      return sheetPreview.getInfo1(s, parents);
  }

  public String getInfo2(Sample s, String parents) {
    if (tabs.getSelectedTab() instanceof BarcodePreviewComponent)
      return tubePreview.getInfo2(s);
    else
      return sheetPreview.getInfo2(s, parents);
  }

  public TabSheet getTabs() {
    return tabs;
  }

  public Button getDownloadButton() {
    return download;
  }

  public List<String> getHeaders() {
    return sheetPreview.getHeaders();
  }

  public void initControl(BarcodeController barcodeController) {
    barcodeController.init(this);
  }

  public void enablePrint(boolean b) {
    this.printTubeCodes.setEnabled(b);
  }

  public void printCommandsDone(PrintReadyRunnable done) {
    if (done.wasSuccess()) {
      Styles.notification("Printing successful", "Your barcodes can be found in the printer room.",
          Styles.NotificationType.SUCCESS);
    } else {
      Styles.notification("Printing error", "There was a problem with contacting the printer.",
          Styles.NotificationType.ERROR);
    }
  }

  public void resetPrinters() {
    this.printerSelection.removeAllItems();
    this.printerSelection.setVisible(false);
  }

  public void setPrinters(Set<Printer> set) {
    printerMap = new HashMap<String, Printer>();
    for (Printer p : set) {
      boolean allowed = !p.isAdminPrinter() || p.isAdminPrinter() && isAdmin;
      if (p.getType().equals(PrinterType.Label_Printer) && allowed) {
        printerMap.put(p.getName() + " (" + p.getLocation() + ")", p);
      }
    }
    if (printerMap.size() > 0) {
      printerSelection.addItems(printerMap.keySet());
    }
  }

  public Printer getPrinter() {
    return printerMap.get(printerSelection.getValue().toString());
  }

  public FilterTable getSampleTable() {
    return sampleTable;
  }

  public boolean printerSelected() {
    return printerSelection.getValue() != null;
  }
}
