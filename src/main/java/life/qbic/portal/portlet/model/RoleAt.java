package life.qbic.portal.portlet.model;

public class RoleAt {

    String affiliation;
    String role;

    public String getAffiliation() {
        return affiliation;
    }

    public String getRole() {
        return role;
    }

    public RoleAt(String affiliation, String role) {
        super();
        this.affiliation = affiliation;
        this.role = role;
    }
}