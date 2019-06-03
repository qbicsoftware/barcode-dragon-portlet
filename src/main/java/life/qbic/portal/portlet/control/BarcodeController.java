package life.qbic.portal.portlet.control;
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


import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import com.liferay.portal.model.UserGroup;
import life.qbic.portal.portlet.util.Tuple;
import life.qbic.portal.Styles;
import life.qbic.portal.portlet.io.BarcodeConfig;
import life.qbic.portal.portlet.io.BarcodeCreator;
import life.qbic.portal.portlet.model.*;
import life.qbic.portal.portlet.processes.SheetBarcodesReadyRunnable;
import life.qbic.portal.portlet.processes.TubeBarcodesReadyRunnable;
import life.qbic.portal.portlet.view.BarcodePreviewComponent;
import life.qbic.portal.portlet.view.PrintReadyRunnable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import life.qbic.portal.portlet.io.DBManager;
import life.qbic.portal.portlet.view.BarcodeView;
import org.apache.commons.lang3.StringUtils;

import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Experiment;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Project;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Sample;
import ch.systemsx.cisd.openbis.plugin.query.shared.api.v1.dto.QueryTableModel;

import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.Extension;
import com.vaadin.ui.Button;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.TabSheet.SelectedTabChangeListener;

import life.qbic.datamodel.identifiers.SampleCodeFunctions;
import life.qbic.datamodel.printing.IBarcodeBean;
import life.qbic.datamodel.printing.NewModelBarcodeBean;
import life.qbic.datamodel.printing.Printer;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.datamodel.sorters.*;
import life.qbic.openbis.openbisclient.IOpenBisClient;
import life.qbic.openbis.openbisclient.OpenBisClient;

/**
 * Controls preparation and creation of barcode files
 *
 * @author Andreas Friedrich
 */

public class BarcodeController implements Observer {

  private BarcodeView view;
  private IOpenBisClient openbis;
  private DBManager dbManager;
  private BarcodeCreator creator;
  private Map<String, Experiment> experimentsMap;
  private List<UserGroup> liferayUserGroupList;
  private String userID;

  List<IBarcodeBean> barcodeBeans;

  private static final Logger LOG = LogManager.getLogger(BarcodeController.class);

  private List<SampleType> barcodeSamples =
      new ArrayList<>(Arrays.asList(SampleType.Q_BIOLOGICAL_SAMPLE, SampleType.Q_TEST_SAMPLE,
          SampleType.Q_NGS_SINGLE_SAMPLE_RUN, SampleType.Q_MHC_LIGAND_EXTRACT, SampleType.Q_MS_RUN,
          SampleType.Q_BMI_GENERIC_IMAGING_RUN));
  // mapping between sample type and interesting property of that sample type has to be added here
  private Map<SampleType, String> sampleTypeToBioTypeField = new HashMap<SampleType, String>() {
    {
      put(SampleType.Q_BIOLOGICAL_SAMPLE, "Q_PRIMARY_TISSUE");
      put(SampleType.Q_TEST_SAMPLE, "Q_SAMPLE_TYPE");
      put(SampleType.Q_NGS_SINGLE_SAMPLE_RUN, "");
      put(SampleType.Q_MHC_LIGAND_EXTRACT, "Q_MHC_CLASS");
      put(SampleType.Q_MS_RUN, "");
      put(SampleType.Q_BMI_GENERIC_IMAGING_RUN, "");
    }
  };

  /**
   * @param bw WizardBarcodeView instance
   * @param openbis OpenBisClient API
   * @param bcConf
   */
  public BarcodeController(BarcodeView bw, OpenBisClient openbis, BarcodeConfig bcConf) {
    // view = bw;
    this.openbis = openbis;
    creator = new BarcodeCreator(bcConf);
  }

  public BarcodeController(IOpenBisClient openbis, BarcodeConfig bcConf, DBManager dbm,
      List<UserGroup> liferayUserGroupList, String userID) {
    this.openbis = openbis;
    this.dbManager = dbm;
    this.liferayUserGroupList = liferayUserGroupList;
    creator = new BarcodeCreator(bcConf);
    this.userID = userID;
  }

  private void sortBeans(List<IBarcodeBean> barcodeBeans) {
    SortBy sorter = view.getSorter();
    switch (sorter) {
      case BARCODE_ID:
        barcodeBeans.sort(SampleCodeComparator.getInstance());
        break;
      case EXT_ID:
        barcodeBeans.sort(SampleExtIDComparator.getInstance());
        break;
      case SAMPLE_TYPE:
        barcodeBeans.sort(SampleTypeComparator.getInstance());
        break;
      case SECONDARY_NAME:
        barcodeBeans.sort(SampleSecondaryNameComparator.getInstance());
        break;
      default:
        LOG.warn("Unknown Barcode Bean sorter or no sorter selected. Barcodes will not be sorted.");
        break;
    }
  }

  /**
   * Initializes all listeners
   */
  @SuppressWarnings("serial")
  public void init(BarcodeView bw) {
    view = bw;

    /**
     * Button listeners
     */
    BarcodeController control = this; // TODO good idea?

    Button.ClickListener cl = (Button.ClickListener) event -> {
      String src = event.getButton().getCaption();

      if (src.startsWith("Print Barcodes")) {
        if (!view.printerSelected()) {
          Styles.notification("No printer selected.", "Please select a printer.",
              Styles.NotificationType.DEFAULT);
        } else {
          view.enablePrint(false);
          String project = view.getProjectCode();
          LOG.info("Sending print command for project " + project + " barcodes");
          Printer p = view.getPrinter();
          creator.printBarcodeFolderForProject(project, p.getHostname(), p.getName(),
              p.getLocation(), view.getSpaceBox().getValue().toString(),
              new PrintReadyRunnable(view), control, userID);
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          view.enablePrint(true);
        }
      }

      if (src.equals("Prepare Barcodes")) {
        if (expSelected()) {
          view.creationPressed();
          Iterator<Extension> it = view.getDownloadButton().getExtensions().iterator();
          if (it.hasNext())
            view.getDownloadButton().removeExtension(it.next());
          barcodeBeans = getBarcodeInfoFromSelections(view.getSelectedExperiments(),
              view.getSelectedSamples());
          // boolean overwrite = view.getOverwrite();
          String project = view.getProjectCode();
          ProgressBar bar = view.getProgressBar();
          bar.setVisible(true);
          sortBeans(barcodeBeans);

          if (view.getTabs().getSelectedTab() instanceof BarcodePreviewComponent) {
            LOG.info("Preparing barcodes (tubes) for project " + project);
            creator.findOrCreateTubeBarcodesWithProgress(barcodeBeans, bar, view.getProgressInfo(),
                new TubeBarcodesReadyRunnable(view, creator, barcodeBeans));
          } else {
            LOG.info("Preparing barcodes (sheet) for project " + project);
            String projectID = "/" + view.getSpaceCode() + "/" + project;
            String name = dbManager.getProjectName(projectID);
            creator.findOrCreateSheetBarcodesWithProgress(barcodeBeans, bar, view.getProgressInfo(),
                new SheetBarcodesReadyRunnable(project, name,
                    dbManager.getPersonForProject(projectID, "PI"),
                    dbManager.getPersonForProject(projectID, "Manager"), view, creator,
                    barcodeBeans));
          }
        } else {
          Styles.notification("Can't create Barcodes",
              "Please select at least one group of Samples from the table!",
              Styles.NotificationType.DEFAULT);
        }
      }
    };

    for (Button b : view.getButtons())
      b.addClickListener(cl);

    /**
     * Space selection listener
     */
    ValueChangeListener spaceSelectListener = (ValueChangeListener) event -> {
      view.resetProjects();
      String space = view.getSpaceCode();
      if (space != null) {
        List<String> projects = new ArrayList<>();
        for (Project p : openbis.getProjectsOfSpace(space)) {
          String code = p.getCode();
          String name = dbManager.getProjectName("/" + space + "/" + code);
          if (name != null && name.length() > 0) {
            if (name.length() >= 80)
              name = name.substring(0, 80) + "...";
            code += " (" + name + ")";
          }
          projects.add(code);
        }
        view.setProjectCodes(projects);
      }
    };
    ComboBox space = view.getSpaceBox();
    if (space != null)
      space.addValueChangeListener(spaceSelectListener);

    /**
     * Project selection listener
     */

    ValueChangeListener projectSelectListener = (ValueChangeListener) event -> {
      view.resetExperiments();
      String project = view.getProjectCode();
      view.resetPrinters();
      view.enablePrep(projSelected());
      if (project != null) {
        if (project.contains(" "))
          project = project.split(" ")[0];
        reactToProjectSelection(project);
      }
    };
    ComboBox project = view.getProjectBox();
    if (project != null)
      project.addValueChangeListener(projectSelectListener);

    /**
     * Experiment selection listener
     */

    ValueChangeListener expSelectListener = (ValueChangeListener) event -> {
      barcodeBeans = null;
      view.resetOptions();
      view.resetSamples();
      view.enablePrep(projSelected());// && optionSelected());
      if (expSelected()) {
        List<Sample> sampleList = new ArrayList<Sample>();
        Map<Sample, String> types = new HashMap<Sample, String>();
        for (ExperimentBarcodeSummary exp : view.getSelectedExperiments()) {
          String type = exp.getBio_Type();
          for (Sample s : exp.getSamples()) {
            sampleList.add(s);
            types.put(s, type);
          }
        }
        view.setSamples(sampleList, types);
        if (tubesSelected())
          view.enableTubeLabelPreview(getUsefulSample());
      } else
        view.disablePreview();
    };
    view.getExperimentTable().addValueChangeListener(expSelectListener);

    ValueChangeListener sampSelectListener = (ValueChangeListener) event -> {
      barcodeBeans = null;
      view.resetOptions();
      view.enablePrep(expSelected());
      if (expSelected() && tubesSelected())
        view.enableTubeLabelPreview(getUsefulSample());
    };
    view.getSampleTable().addValueChangeListener(sampSelectListener);

    SelectedTabChangeListener tabListener = (SelectedTabChangeListener) event -> {
      view.resetOptions();
      view.enablePrep(projSelected());
      if (tubesSelected() && expSelected())
        view.enableTubeLabelPreview(getUsefulSample());
      else
        view.disablePreview();
    };
    view.getTabs().addSelectedTabChangeListener(tabListener);
  }

  // table selection is sorted by sample registration date
  private void reactToProjectSelection(String project) {
    Map<Tuple, ExperimentBarcodeSummary> experiments = new HashMap<>();
    view.setPrinters(dbManager.getPrintersForProject(project, liferayUserGroupList));

    experimentsMap = new HashMap<>();
    String projectID = "/" + view.getSpaceCode() + "/" + project;
    for (Experiment e : openbis.getExperimentsForProject(projectID)) {
      experimentsMap.put(e.getIdentifier(), e);
    }
    for (Sample s : openbis.getSamplesWithParentsAndChildrenOfProjectBySearchService(projectID)) {

      SampleType type = parseSampleType(s);
      if (barcodeSamples.contains(type) && SampleCodeFunctions.isQbicBarcode(s.getCode())) {
        Date date = s.getRegistrationDetails().getRegistrationDate();
        SimpleDateFormat dt1 = new SimpleDateFormat("yy-MM-dd");
        String dt = dt1.format(date);
        String expID = s.getExperimentIdentifierOrNull();
        String expName = experimentsMap.get(expID).getProperties().get("Q_SECONDARY_NAME");
        if (expName == null || expName.isEmpty())
          expName = expID;
        Tuple tpl = new Tuple(dt, expID);
        if (experiments.containsKey(tpl)) {
          ExperimentBarcodeSummary exp = experiments.get(tpl);
          exp.increment();
          exp.addSample(s);
        } else {
          String bioType = null;
          switch (type) {
            case Q_BIOLOGICAL_SAMPLE:
              bioType = "Tissue Extracts";
              break;
            case Q_TEST_SAMPLE:
              bioType = s.getProperties().get("Q_SAMPLE_TYPE");
              break;
            case Q_NGS_SINGLE_SAMPLE_RUN:
              bioType =
                  openbis.getExperimentById2(expID).get(0).getProperties().get("Q_SEQUENCING_TYPE")
                      + "seq";
              break;
            case Q_MHC_LIGAND_EXTRACT:
              bioType = "MHC Ligands";
              break;
            // normal MS Runs use the Barcodes of Peptides or Proteins to register data and so are
            // not listed here
            case Q_MS_RUN:
              bioType = "Wash Runs";
              break;
            case Q_BMI_GENERIC_IMAGING_RUN:
              bioType =
                  openbis.getExperimentById2(expID).get(0).getProperties().get("Q_BMI_MODALITY")
                      + " runs";
            default:
              break;
          }
          ExperimentBarcodeSummary b = new ExperimentBarcodeSummary(bioType, "1", expName, dt);
          b.addSample(s);
          experiments.put(tpl, b);
        }
      }
    }
    view.setExperiments(experiments.values());
  }

  private SampleType parseSampleType(Sample s) {
    SampleType type = null;
    try {
      type = SampleType.valueOf(s.getSampleTypeCode());
    } catch (IllegalArgumentException e) {
      LOG.warn(s.getSampleTypeCode()
          + " does not seem to be a supported sample type. Consider adding it to the data model library enums.");
    }
    return type;
  }

  private boolean isBlankOrWash(Sample s) {
    return SampleCodeFunctions.isMeasurementOfBarcode(s.getCode(), s.getSampleTypeCode())
        && s.getParents().isEmpty();
  }

  @Override
  public void update(Observable o, Object arg) {
    view.enableTubeLabelPreview(getUsefulSample());
  }

  private Sample getUsefulSample() {
    List<Sample> samples = view.getSelectedSamples();
    if (samples.isEmpty())
      samples = view.getSelectedExperiments().iterator().next().getSamples();
    int i = 0;
    String code = samples.get(i).getCode();
    while (!SampleCodeFunctions.isQbicBarcode(code)) {
      code = samples.get(i).getCode();
      i++;
    }

    return samples.get(i);
  }

  private boolean tubesSelected() {
    return view.getTabs().getSelectedTab() instanceof BarcodePreviewComponent;
  }

  private boolean projSelected() {
    return view.getProjectBox().getValue() != null;
  }

  private boolean expSelected() {
    return view.getSelectedExperiments().size() > 0;
  }

  private List<IBarcodeBean> getBarcodeInfoFromSelections(
      Collection<ExperimentBarcodeSummary> experiments, List<Sample> samples) {
    List<IBarcodeBean> sampleBarcodes = new ArrayList<>();
    List<Sample> openbisSamples = new ArrayList<>();
    for (ExperimentBarcodeSummary b : experiments) {
      openbisSamples.addAll(b.getSamples());
    }
    // the subselection of samples is used instead of the whole experiments
    // if at least one of them and less than all of them are selected (no one wants to prepare 0
    // samples)
    if (samples.size() < openbisSamples.size() && samples.size() != 0)
      openbisSamples = samples;
    // Map<Sample, List<String>> parentMap = getParentMap(openbisSamples);
    for (Sample s : openbisSamples) {
      SampleType type = parseSampleType(s);
      String bioType = "unknown";

      if (sampleTypeToBioTypeField.containsKey(type)) {
        String typeKey = sampleTypeToBioTypeField.get(type);
        if (s.getProperties().containsKey(typeKey)) {
          bioType = s.getProperties().get(typeKey);
        } else {
          switch (type) {
            case Q_MS_RUN:
              bioType = "NGS RUN";
              break;
            case Q_BMI_GENERIC_IMAGING_RUN:
              bioType = "Imaging RUN";
              break;
            default:
          }
        }
      }
      List<String> parents = parentsToStringList(s.getParents());
      String parentString = StringUtils.join(parents, " ");
      sampleBarcodes.add(new NewModelBarcodeBean(s.getCode(), view.getCodedString(s),
          view.getInfo1(s, parentString), view.getInfo2(s, parentString), bioType, parents,
          s.getProperties().get("Q_SECONDARY_NAME"), s.getProperties().get("Q_EXTERNALDB_ID")));
    }

    return sampleBarcodes;
  }

  private List<String> parentsToStringList(List<Sample> samples) {
    List<String> res = new ArrayList<String>();
    for (Sample s : samples) {
      res.add(s.getCode());
    }

    return res;
  }

  protected Map<Sample, List<String>> getParentMap(List<Sample> samples) {
    List<String> codes = new ArrayList<>();
    for (Sample s : samples) {
      codes.add(s.getCode());
    }
    Map<String, Object> params = new HashMap<>();
    params.put("codes", codes);
    QueryTableModel resTable = openbis.getAggregationService("get-parentmap", params);
    Map<String, List<String>> parentMap = new HashMap<>();

    for (Serializable[] ss : resTable.getRows()) {
      String code = (String) ss[0];
      String parent = (String) ss[1];
      if (parentMap.containsKey(code)) {
        List<String> parents = parentMap.get(code);
        parents.add(parent);
        parentMap.put(code, parents);
      } else {
        parentMap.put(code, new ArrayList<>(Arrays.asList(parent)));
      }
    }
    Map<Sample, List<String>> res = new HashMap<>();
    for (Sample s : samples) {
      List<String> prnts = parentMap.get(s.getCode());
      if (prnts == null)
        prnts = new ArrayList<>();
      res.put(s, prnts);
    }

    return res;
  }

  public DBManager getDbManager() {
    return dbManager;
  }
}
