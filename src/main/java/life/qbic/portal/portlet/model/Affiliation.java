package life.qbic.portal.portlet.model;

public class Affiliation {

  private int id;
  private String organization;
  private String addressAddition;
  private String street;
  private String zipCode;
  private String city;
  private String country;

  public Affiliation(int id, String organization, String addressAddition, String street,
      String zipCode, String city, String country) {
    this.id = id;
    this.organization = organization;
    this.addressAddition = addressAddition;
    this.zipCode = zipCode;
    this.street = street;
    this.city = city;
    this.country = country;
  }

  public int getID() {
    return id;
  }

  public String getAddressAddition() {
    return addressAddition;
  }

  public String getOrganization() {
    return organization;
  }

  public String getStreet() {
    return street;
  }

  public String getZipCode() {
    return zipCode;
  }

  public String getCity() {
    return city;
  }

  public String getCountry() {
    return country;
  }
}
