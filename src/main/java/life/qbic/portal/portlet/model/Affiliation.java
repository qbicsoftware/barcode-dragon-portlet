package life.qbic.portal.portlet.model;

public class Affiliation {

    private int id;
    private String groupName;
    private String acronym;
    private String institute;
    private String organization;
    private String faculty;
    private int contactPersonID;
    private int headID;
    private String contactPerson;
    private String headName;
    private String street;
    private String zipCode;
    private String city;
    private String country;
    private String webpage;

    public Affiliation(String groupName, String acronym, String organization, String institute,
                       String faculty, int contactID, int headID, String street, String zipCode, String city,
                       String country, String webpage) {
        this.groupName = groupName;
        this.acronym = acronym;
        this.organization = organization;
        this.institute = institute;
        this.faculty = faculty;
        this.contactPersonID = contactID;
        this.headID = headID;
        this.street = street;
        this.zipCode = zipCode;
        this.city = city;
        this.country = country;
        this.webpage = webpage;
    }

    public Affiliation(int id, String groupName, String acronym, String organization,
                       String institute, String faculty, String contact, String head, String street, String zipCode,
                       String city, String country, String webpage) {
        this.id = id;
        this.groupName = groupName;
        this.acronym = acronym;
        this.organization = organization;
        this.institute = institute;
        this.faculty = faculty;
        this.contactPerson = contact;
        this.headName = head;
        this.street = street;
        this.zipCode = zipCode;
        this.city = city;
        this.country = country;
        this.webpage = webpage;
    }

    public int getID() {
        return id;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getAcronym() {
        return acronym;
    }

    public String getOrganization() {
        return organization;
    }

    public String getInstitute() {
        return institute;
    }

    public String getFaculty() {
        return faculty;
    }

    public int getContactPersonID() {
        return contactPersonID;
    }

    public int getHeadID() {
        return headID;
    }

    public String getContactPerson() {
        return contactPerson;
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

    public String getWebpage() {
        return webpage;
    }

    public String getHeadName() {
        return headName;
    }
}
