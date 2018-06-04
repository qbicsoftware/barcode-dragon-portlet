package life.qbic.portal.portlet.model;

/**
 * Enum type describing possible ways to sort ISampleBeans
 * 
 * @author Andreas Friedrich
 * 
 */
public enum SortBy {
  BARCODE_ID("Barcode ID (recommended)"), EXT_ID("Lab ID"), SECONDARY_NAME("Secondary Name"), SAMPLE_TYPE(
      "Tissue/Source");

  private String readable;

  private SortBy(String value) {
    this.readable = value;
  }

  public String getValue() {
    return readable;
  }

  @Override
  public String toString() {
    return readable;
  }
}
