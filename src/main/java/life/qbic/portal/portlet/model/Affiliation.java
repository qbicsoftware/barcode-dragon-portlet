package life.qbic.portal.portlet.model;

public class Affiliation {

  private int id;
  private String group;
  private String acronym;
  private String organization;
  private String institute;
  private String street;
  private String city;
  private String zip;
  private String country;

  public Affiliation(int id, String group, String acronym, String organization, String institute,
      String street, String city, String zip, String country) {
    super();
    this.id = id;
    this.group = group;
    this.acronym = acronym;
    this.organization = organization;
    this.institute = institute;
    this.street = street;
    this.city = city;
    this.zip = zip;
    this.country = country;
  }

  public String getSheetInfoString() {
    String address = "";
    if (group == null || group.toUpperCase().equals("NULL") || group.equals("")) {
      if (institute == null || institute.toUpperCase().equals("NULL") || institute.equals("")) {
        address = organization;
      } else {
        address = institute;
      }
    } else {
      address = group;
      if (acronym != null && !acronym.isEmpty())
        address += " (" + acronym + ")";
    }
    address += ";" + street;
    address += ";" + zip + " " + city;
    address += ";" + country;
    return address;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public String getAcronym() {
    return acronym;
  }

  public void setAcronym(String acronym) {
    this.acronym = acronym;
  }

  public String getOrganization() {
    return organization;
  }

  public void setOrganization(String organization) {
    this.organization = organization;
  }

  public String getInstitute() {
    return institute;
  }

  public void setInstitute(String institute) {
    this.institute = institute;
  }

  public String getStreet() {
    return street;
  }

  public void setStreet(String street) {
    this.street = street;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getZip() {
    return zip;
  }

  public void setZip(String zip) {
    this.zip = zip;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

}
