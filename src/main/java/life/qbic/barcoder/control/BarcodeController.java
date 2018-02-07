package life.qbic.barcoder.control;
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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import com.liferay.portal.model.UserGroup;
import life.qbic.barcoder.helpers.Styles;
import life.qbic.barcoder.helpers.Styles.NotificationType;
import life.qbic.barcoder.helpers.Tuple;
import life.qbic.barcoder.io.BarcodeConfig;
import life.qbic.barcoder.io.BarcodeCreator;
import life.qbic.barcoder.logging.Log4j2Logger;
import life.qbic.barcoder.logging.Logger;
import life.qbic.barcoder.model.IBarcodeBean;
import life.qbic.barcoder.model.NewModelBarcodeBean;
import life.qbic.barcoder.sorters.SampleTypeComparator;
import life.qbic.barcoder.view.BarcodePreviewComponent;
import life.qbic.barcoder.view.BarcodeView;
import life.qbic.barcoder.view.PrintReadyRunnable;
import org.apache.commons.lang3.StringUtils;

import life.qbic.barcoder.model.ExperimentBarcodeSummary;
import life.qbic.barcoder.model.Printer;
import life.qbic.barcoder.sorters.SampleCodeComparator;
import life.qbic.barcoder.model.SortBy;
import life.qbic.barcoder.processes.SheetBarcodesReadyRunnable;
import life.qbic.barcoder.processes.TubeBarcodesReadyRunnable;
import life.qbic.barcoder.sorters.SampleExtIDComparator;
import life.qbic.barcoder.sorters.SampleSecondaryNameComparator;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Experiment;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Project;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Sample;
import ch.systemsx.cisd.openbis.plugin.query.shared.api.v1.dto.QueryTableModel;
import life.qbic.barcoder.helpers.Functions;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.Extension;
import com.vaadin.ui.Button;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.TabSheet.SelectedTabChangeEvent;
import com.vaadin.ui.TabSheet.SelectedTabChangeListener;

import life.qbic.barcoder.io.DBManager;
import life.qbic.openbis.openbisclient.IOpenBisClient;
import life.qbic.openbis.openbisclient.OpenBisClient;

/**
 * Controls preparation and creation of barcode files
 * 
 * @author Andreas Friedrich
 * 
 */

public class BarcodeController implements Observer {

  private BarcodeView view;
  private IOpenBisClient openbis;
  private DBManager dbManager;
  private BarcodeCreator creator;
  private Map<String, Experiment> experimentsMap;
  private List<UserGroup> liferayUserGroupList;

  List<IBarcodeBean> barcodeBeans;

  Logger logger = new Log4j2Logger(BarcodeController.class);

  private List<String> barcodeExperiments =
      new ArrayList<String>(Arrays.asList("Q_SAMPLE_EXTRACTION", "Q_SAMPLE_PREPARATION",
          "Q_NGS_MEASUREMENT", "Q_MHC_LIGAND_EXTRACTION", "Q_MS_MEASUREMENT"));
  private List<String> barcodeSamples = new ArrayList<String>(Arrays.asList("Q_BIOLOGICAL_SAMPLE",
      "Q_TEST_SAMPLE", "Q_NGS_SINGLE_SAMPLE_RUN", "Q_MHC_LIGAND_EXTRACT", "Q_MS_RUN"));
  // mapping between sample type and interesting property of that sample type has to be added here
  private Map<String, String> sampleTypeToBioTypeField = new HashMap<String, String>() {
    {
      put("Q_BIOLOGICAL_SAMPLE", "Q_PRIMARY_TISSUE");
      put("Q_TEST_SAMPLE", "Q_SAMPLE_TYPE");
      put("Q_NGS_SINGLE_SAMPLE_RUN", "");
      put("Q_MHC_LIGAND_EXTRACT", "Q_MHC_CLASS");
      put("Q_MS_RUN", "");
    }
  };

  /**
   * @param bw WizardBarcodeView instance
   * @param openbis OpenBisClient API
   * @param barcodeScripts Path to different barcode creation scripts
   * @param pathVar Path variable so python scripts can work when called from the JVM
   */
  public BarcodeController(BarcodeView bw, OpenBisClient openbis, BarcodeConfig bcConf) {
    // view = bw;
    this.openbis = openbis;
    creator = new BarcodeCreator(bcConf);
  }

  public BarcodeController(IOpenBisClient openbis, BarcodeConfig bcConf, DBManager dbm,
      List<UserGroup> liferayUserGroupList) {
    this.openbis = openbis;
    this.dbManager = dbm;
    this.liferayUserGroupList = liferayUserGroupList;
    creator = new BarcodeCreator(bcConf);
  }

  private void sortBeans(List<IBarcodeBean> barcodeBeans) {
    SortBy sorter = view.getSorter();
    switch (sorter) {
      case BARCODE_ID:
        Collections.sort(barcodeBeans, SampleCodeComparator.getInstance());
        break;
      case EXT_ID:
        Collections.sort(barcodeBeans, SampleExtIDComparator.getInstance());
        break;
      case SAMPLE_TYPE:
        Collections.sort(barcodeBeans, SampleTypeComparator.getInstance());
        break;
      case SECONDARY_NAME:
        Collections.sort(barcodeBeans, SampleSecondaryNameComparator.getInstance());
        break;
      default:
        logger.warn(
            "Unknown Barcode Bean sorter or no sorter selected. Barcodes will not be sorted.");
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
    Button.ClickListener cl = new Button.ClickListener() {
      @Override
      public void buttonClick(ClickEvent event) {
        String src = event.getButton().getCaption();
        if (src.startsWith("Print Barcodes")) {
          Printer p = view.getPrinter();
          if (p == null) {
            Styles.notification("No printer selected.", "Please select a printer!",
                NotificationType.DEFAULT);
          } else {
            view.enablePrint(false);
            String project = view.getProjectCode();
            logger.info("Sending print command for project " + project + " barcodes");
            creator.printBarcodeFolderForProject(project, p.getHostname(), p.getName(),
                new PrintReadyRunnable(view));
            try {
              Thread.sleep(1000);
            } catch (InterruptedException e) {
              // TODO Auto-generated catch block
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
              logger.info("Preparing barcodes (tubes) for project " + project);
              creator.findOrCreateTubeBarcodesWithProgress(barcodeBeans, bar,
                  view.getProgressInfo(),
                  new TubeBarcodesReadyRunnable(view, creator, barcodeBeans));
            } else {
              logger.info("Preparing barcodes (sheet) for project " + project);
              String projectID = "/" + view.getSpaceCode() + "/" + project;
              String name = dbManager.getProjectName(projectID);
              creator.findOrCreateSheetBarcodesWithProgress(barcodeBeans, bar,
                  view.getProgressInfo(),
                  new SheetBarcodesReadyRunnable(project, name,
                      dbManager.getPersonForProject(projectID, "PI"),
                      dbManager.getPersonForProject(projectID, "Contact"), view, creator,
                      barcodeBeans));
            }
          } else
            Styles.notification("Can't create Barcodes",
                "Please select at least one group of Sampes from the table!",
                Styles.NotificationType.DEFAULT);
        }
      }
    };
    for (Button b : view.getButtons())
      b.addClickListener(cl);

    /**
     * Space selection listener
     */
    ValueChangeListener spaceSelectListener = new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        view.resetProjects();
        String space = view.getSpaceCode();
        if (space != null) {
          List<String> projects = new ArrayList<String>();
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
      }

    };
    ComboBox space = view.getSpaceBox();
    if (space != null)
      space.addValueChangeListener(spaceSelectListener);

    /**
     * Project selection listener
     */

    ValueChangeListener projectSelectListener = new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        view.resetExperiments();
        String project = view.getProjectCode();
        view.setAvailableTubes(0);
        view.resetPrinters();
        view.enablePrep(projSelected());
        if (project != null) {
          if (project.contains(" "))
            project = project.split(" ")[0];
          reactToProjectSelection(project);
        }
      }

    };
    ComboBox project = view.getProjectBox();
    if (project != null)
      project.addValueChangeListener(projectSelectListener);

    /**
     * Experiment selection listener
     */

    ValueChangeListener expSelectListener = new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
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
      }
    };
    view.getExperimentTable().addValueChangeListener(expSelectListener);

    ValueChangeListener sampSelectListener = new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        barcodeBeans = null;
        view.resetOptions();
        view.enablePrep(expSelected());
        if (expSelected() && tubesSelected())
          view.enableTubeLabelPreview(getUsefulSample());
      }
    };
    view.getSampleTable().addValueChangeListener(sampSelectListener);

    SelectedTabChangeListener tabListener = new SelectedTabChangeListener() {
      @Override
      public void selectedTabChange(SelectedTabChangeEvent event) {
        view.resetOptions();
        view.enablePrep(projSelected());
        if (tubesSelected() && expSelected())
          view.enableTubeLabelPreview(getUsefulSample());
        else
          view.disablePreview();
      }
    };
    view.getTabs().addSelectedTabChangeListener(tabListener);
  }

  // table selection is sorted by sample registration date
  public void reactToProjectSelection(String project) {
    Map<Tuple, ExperimentBarcodeSummary> experiments =
        new HashMap<Tuple, ExperimentBarcodeSummary>();
    view.setPrinters(dbManager.getPrintersForProject(project, liferayUserGroupList));

    experimentsMap = new HashMap<String, Experiment>();
    String projectID = "/" + view.getSpaceCode() + "/" + project;
    for (Experiment e : openbis.getExperimentsForProject(projectID)) {
      experimentsMap.put(e.getIdentifier(), e);
    }
    for (Sample s : openbis.getSamplesWithParentsAndChildrenOfProjectBySearchService(projectID)) {
      String type = s.getSampleTypeCode();
      if (barcodeSamples.contains(type) && Functions.isQbicBarcode(s.getCode())) {

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
          if (type.equals(barcodeSamples.get(0)))
            bioType = "Tissue Extracts";
          else if (type.equals(barcodeSamples.get(1)))
            bioType = s.getProperties().get("Q_SAMPLE_TYPE");
          else if (type.equals(barcodeSamples.get(2)))
            bioType =
                openbis.getExperimentById2(expID).get(0).getProperties().get("Q_SEQUENCING_TYPE")
                    + "seq";
          else if (type.equals(barcodeSamples.get(3)))
            bioType = "MHC Ligands";
          // ms run
          else if (type.equals(barcodeSamples.get(4)))
            bioType = "Wash Runs";
          ExperimentBarcodeSummary b = new ExperimentBarcodeSummary(bioType, "1", expName, dt);
          b.addSample(s);
          experiments.put(tpl, b);
        }
      }
    }
    view.setExperiments(experiments.values());
  }

  private boolean isBlankOrWash(Sample s) {
    return Functions.isMeasurementOfBarcode(s.getCode(), s.getSampleTypeCode())
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
    while (!Functions.isQbicBarcode(code)) {
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

  protected List<IBarcodeBean> getBarcodeInfoFromSelections(
      Collection<ExperimentBarcodeSummary> experiments, List<Sample> samples) {
    List<IBarcodeBean> sampleBarcodes = new ArrayList<IBarcodeBean>();
    List<Sample> openbisSamples = new ArrayList<Sample>();
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
      String type = s.getSampleTypeCode();
      String bioType = "unknown";
      if (sampleTypeToBioTypeField.containsKey(type)) {
        if (sampleTypeToBioTypeField.get(type).isEmpty())
          bioType = "NGS RUN";
        else
          bioType = s.getProperties().get(sampleTypeToBioTypeField.get(type));
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
    List<String> codes = new ArrayList<String>();
    for (Sample s : samples) {
      codes.add(s.getCode());
    }
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("codes", codes);
    QueryTableModel resTable = openbis.getAggregationService("get-parentmap", params);
    Map<String, List<String>> parentMap = new HashMap<String, List<String>>();

    for (Serializable[] ss : resTable.getRows()) {
      String code = (String) ss[0];
      String parent = (String) ss[1];
      if (parentMap.containsKey(code)) {
        List<String> parents = parentMap.get(code);
        parents.add(parent);
        parentMap.put(code, parents);
      } else {
        parentMap.put(code, new ArrayList<String>(Arrays.asList(parent)));
      }
    }
    Map<Sample, List<String>> res = new HashMap<Sample, List<String>>();
    for (Sample s : samples) {
      List<String> prnts = parentMap.get(s.getCode());
      if (prnts == null)
        prnts = new ArrayList<String>();
      res.put(s, prnts);
    }
    return res;
  }

}
