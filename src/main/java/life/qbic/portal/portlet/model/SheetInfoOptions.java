package life.qbic.portal.portlet.model;

public enum SheetInfoOptions implements IBarcodeOptions {

  Extract_Material("Tissue/Extr. Material"), Secondary_Name("Secondary Name"), QBIC_Code(
      "QBiC Code"), Lab_ID("Lab ID"), Parent_Samples("Parent Samples (Source)");

  private final String name;

  SheetInfoOptions(String s) {
    name = s;
  }

  public boolean equalsName(String otherName) {
    return name.equals(otherName);
  }

  public String toString() {
    return this.name;
  }
  
  public static SheetInfoOptions fromString(String name) {
    for (SheetInfoOptions b : SheetInfoOptions.values()) {
      if (b.name.equalsIgnoreCase(name)) {
        return b;
      }
    }
    return null;
  }

}
