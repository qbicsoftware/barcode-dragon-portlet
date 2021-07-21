package life.qbic.portal.portlet.model;

public enum QRInfoOptions implements IBarcodeOptions {

  Extract_Material("Tissue/Extr. Material"), Species("Organism"), Secondary_Name(
      "Secondary Name"), QBIC_Code(
          "QBiC Code"), Lab_ID("Lab ID"), MHC_Type("MHC Type"), Antibody("Used Antibody");

  private final String name;

  QRInfoOptions(String s) {
    name = s;
  }

  public boolean equalsName(String otherName) {
    return name.equals(otherName);
  }

  public String toString() {
    return this.name;
  }

  public static QRInfoOptions fromString(String name) {
    for (QRInfoOptions b : QRInfoOptions.values()) {
      if (b.name.equalsIgnoreCase(name)) {
        return b;
      }
    }
    return null;
  }

}
