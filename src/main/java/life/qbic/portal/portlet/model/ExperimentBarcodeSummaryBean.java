package life.qbic.portal.portlet.model;

/**
 * Bean Object representing an experiment with some information about its samples to provide an
 * overview, e.g. for barcode creation
 * 
 * @author Andreas Friedrich
 * 
 */
public class ExperimentBarcodeSummaryBean {

  String Bio_Type;
  String Amount;
  String experimentID;

  /**
   * Creates a new ExperimentBarcodeSummaryBean
   * 
   * @param bioType the type of samples in this experiment, for example tissue or measurement type
   * @param amount the amount of samples in this experiment
   */
  public ExperimentBarcodeSummaryBean(String bioType, String amount, String expID) {
    Bio_Type = bioType;
    Amount = amount;
    this.experimentID = expID;
  }

  // show only code
  public String getExperiment() {
    String[] split = experimentID.split("/");
    return split[split.length - 1];
  }

  public String fetchExperimentID() {
    return experimentID;
  }

  public String getBio_Type() {
    return Bio_Type;
  }

  public void setBio_Type(String bio_Type) {
    Bio_Type = bio_Type;
  }

  public String getAmount() {
    return Amount;
  }

  public void setAmount(String amount) {
    Amount = amount;
  }

}
