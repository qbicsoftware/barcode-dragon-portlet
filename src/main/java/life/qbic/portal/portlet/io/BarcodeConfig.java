package life.qbic.portal.portlet.io;

public class BarcodeConfig {
  
  private String scriptsFolder;
  private String tmpFolder;
  private String resultsFolder;
  private String pathVar;
  
  public BarcodeConfig(String scriptsFolder, String tmpFolder, String resultsFolder, String pathVar) {
    super();
    this.scriptsFolder = scriptsFolder;
    this.tmpFolder = tmpFolder;
    this.resultsFolder = resultsFolder;
    this.pathVar = pathVar;
  }
  
  public String getScriptsFolder() {
    return scriptsFolder;
  }
  public String getTmpFolder() {
    return tmpFolder;
  }
  public String getResultsFolder() {
    return resultsFolder;
  }
  public String getPathVar() {
    return pathVar;
  }

}
