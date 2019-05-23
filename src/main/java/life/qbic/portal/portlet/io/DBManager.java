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
package life.qbic.portal.portlet.io;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import com.liferay.portal.model.UserGroup;
import com.vaadin.data.Property;
import com.vaadin.data.util.sqlcontainer.SQLContainer;
import com.vaadin.data.util.sqlcontainer.connection.JDBCConnectionPool;
import com.vaadin.data.util.sqlcontainer.connection.SimpleJDBCConnectionPool;
import com.vaadin.data.util.sqlcontainer.query.FreeformQuery;
import java.util.concurrent.atomic.AtomicInteger;

import life.qbic.datamodel.printing.Printer;
import life.qbic.portal.portlet.model.Affiliation;
import life.qbic.portal.portlet.model.Person;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class should be used as a singleton, but it is not so easy to enforce due to Dr. Spagghet Ticode (01.10.18, LG)
 */
public class DBManager {

  private static final Logger LOG = LogManager.getLogger(DBManager.class);
  private static final AtomicInteger N_INSTANCES = new AtomicInteger(0);

  private final JDBCConnectionPool connectionPool;

  public DBManager(final DBConfig config) {
    final int nInstances = N_INSTANCES.incrementAndGet();
    if (nInstances > 1) {
      LOG.error("There are {} instances of DBManager right now. DBManager should be a singleton.", nInstances);
    }
    this.connectionPool = createConnectionPool(config);
  }

  private JDBCConnectionPool createConnectionPool(final DBConfig config) {
    try {
      return new SimpleJDBCConnectionPool(
          "org.mariadb.jdbc.Driver", "jdbc:mariadb://" + config.getHostname() + ":"
          + config.getPort() + "/" + config.getSql_database(),
          config.getUsername(), config.getPassword(), 1, 20);
    } catch (SQLException e) {
      LOG.error("Could not create connection pool", e);
      throw new RuntimeException(e);
    }
  }

  private void logout(Connection conn) {
    Validate.notNull(conn, "connection cannot be null, this seems to be a bug and should be reported");
    connectionPool.releaseConnection(conn);
  }

  private Connection login() {
    try {
      return connectionPool.reserveConnection();
    } catch (SQLException e) {
      LOG.error("Could not get connection from pool", e);
      throw new RuntimeException(e);
    }
  }

  public Person getPersonForProject(String projectIdentifier, String role) {
    String sql =
        "SELECT * FROM persons LEFT JOIN projects_persons ON persons.id = projects_persons.person_id "
            + "LEFT JOIN projects ON projects_persons.project_id = projects.id WHERE "
            + "projects.openbis_project_identifier = ? AND projects_persons.project_role = ?";
    Person res = null;

    Connection conn = login();
    try (PreparedStatement statement = conn.prepareStatement(sql)) {
      statement.setString(1, projectIdentifier);
      statement.setString(2, role);

      ResultSet rs = statement.executeQuery();


      while (rs.next()) {
        String title = rs.getString("title");
        String zdvID = rs.getString("username");
        String first = rs.getString("first_name");
        String last = rs.getString("family_name");
        String email = rs.getString("email");
        String tel = rs.getString("phone");
        Affiliation affiliation = getAffiliationFromProjectIDAndRole(projectIdentifier, role);
        int instituteID = -1;// TODO fetch correct id


        res = new Person(zdvID, title, first, last, email, tel, instituteID, affiliation);
      }
    } catch (SQLException e) {
      LOG.error("Could not get person for project due to database error", e);
    } finally {
      logout(conn);
    }

    return res;
  }

  /**
   *
   *
   * @param id Affiliation ID!
   * @return Found Affiliation with address, group etc
   */
  private Affiliation getAffiliationWithID(int id) {
    Affiliation res = null;
    String sql = "SELECT * from organizations WHERE id = ?";

    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      statement.setInt(1, id);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        String groupName = rs.getString("group_name");
        String acronym = rs.getString("group_acronym");
        if (acronym == null)
          acronym = "";
        String organization = rs.getString("umbrella_organization");
        String faculty = rs.getString("faculty");
        String institute = rs.getString("institute");
        if (institute == null)
          institute = "";
        String street = rs.getString("street");
        String zipCode = rs.getString("zip_code");
        String city = rs.getString("city");
        String country = rs.getString("country");
        String webpage = rs.getString("webpage");
        int contactID = rs.getInt("main_contact");
        int headID = rs.getInt("head");
        String contact = null;
        String head = null;


        res = new Affiliation(id, groupName, acronym, organization, institute, faculty, contact,
                head, street, zipCode, city, country, webpage);
      }
    } catch (SQLException e) {
      LOG.error("Could not get affiliation with ID", e);
    } finally {
      logout(conn);
    }
    return res;
  }

  /**
   *
   * @param personID
   * @return AffiliationID, forwarded to getAffiliationWithID
   */
  public int getAffiliationIDForPersonID(Integer personID) {
    String lnk = "persons_organizations";
    String sql =
            "SELECT persons.*, organizations.*, " + lnk + ".occupation FROM persons, organizations, "
                    + lnk + " WHERE persons.id = " + Integer.toString(personID) + " AND persons.id = "
                    + lnk + ".person_id and organizations.id = " + lnk + ".organization_id";
    Connection conn = login();

    int affiliationID = -1;

    try (PreparedStatement statement = conn.prepareStatement(sql)) {
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
         affiliationID = rs.getInt("organizations.id");

      }
      statement.close();
    } catch (SQLException e) {
      LOG.error("Could not get affiliation ID", e);
    } finally {
      logout(conn);
    }

    return affiliationID;
  }


  public String getAffiliationOfRoleOfProject(String projectIdentifier, String role) {
    String sql =
            "SELECT projects_persons.*, projects.* FROM projects_persons, projects WHERE projects.openbis_project_identifier = ?"
                    + " AND projects.id = projects_persons.project_id AND projects_persons.project_role = ?";

    int id = -1;

    Connection conn = login();
    try (PreparedStatement statement = conn.prepareStatement(sql)) {
      statement.setString(1, projectIdentifier);
      statement.setString(2, role);

      ResultSet rs = statement.executeQuery();

      while (rs.next()) {
        id = rs.getInt("person_id");
      }

      String personAffiliation = getPersonsAffiliation(id);
      return personAffiliation;

    } catch (SQLException e) {
      LOG.error("Could not get affiliation of role of project", e);
    } finally {
      logout(conn);
    }

    // haha for real?
    return "";
  }

  /**
   * Fetches AffiliationID from PersonID
   * Then uses the AffiliationID to get the Affiliation
   *
   * @param projectIdentifier
   * @param role
   * @return Affiliation
   */
  public Affiliation getAffiliationFromProjectIDAndRole(String projectIdentifier, String role) {
    String sql =
            "SELECT projects_persons.*, projects.* FROM projects_persons, projects WHERE projects.openbis_project_identifier = ?"
                    + " AND projects.id = projects_persons.project_id AND projects_persons.project_role = ?";

    int id = -1;
    Affiliation affiliationOfPerson = null;

    Connection conn = login();
    try (PreparedStatement statement = conn.prepareStatement(sql)) {
      statement.setString(1, projectIdentifier);
      statement.setString(2, role);

      ResultSet rs = statement.executeQuery();

      while (rs.next()) {
        id = rs.getInt("person_id");
      }

      int affiliationID = getAffiliationIDForPersonID(id);
      affiliationOfPerson = getAffiliationWithID(affiliationID);


    } catch (SQLException e) {
      LOG.error("Could not get affiliation from project", e);
    } finally {
      logout(conn);
    }

    return affiliationOfPerson;
  }

    public String getPersonsAffiliation(Integer personID) {
      String affiliation = null;
      String lnk = "persons_organizations";
      String sql =
              "SELECT persons.*, organizations.*, " + lnk + ".occupation FROM persons, organizations, "
                      + lnk + " WHERE persons.id = " + Integer.toString(personID) + " AND persons.id = "
                      + lnk + ".person_id and organizations.id = " + lnk + ".organization_id";
      Connection conn = login();
      try (PreparedStatement statement = conn.prepareStatement(sql)) {
        ResultSet rs = statement.executeQuery();
        while (rs.next()) {

          int affiliationID = rs.getInt("organizations.id");

          String group_acronym = rs.getString("group_acronym");
          String group_name = rs.getString("group_name");
          String institute = rs.getString("institute");
          String organization = rs.getString("umbrella_organization");
          affiliation = "";

          if (group_name == null || group_name.toUpperCase().equals("NULL") || group_name.equals("")) {

            if (institute == null || institute.toUpperCase().equals("NULL") || institute.equals("")) {
              affiliation = organization;
            } else {
              affiliation = institute;
            }

          } else {
            affiliation = group_name;
            if (group_acronym != null && !group_acronym.isEmpty())
              affiliation += " (" + group_acronym + ")";
          }

          String role = rs.getString(lnk + ".occupation");

        }

      } catch (SQLException e) {
        LOG.error("Could not get person affiliation", e);
      } finally {
        logout(conn);
      }

      return affiliation;
    }

  public String getProjectName(String projectIdentifier) {
    String sql = "SELECT short_title from projects WHERE openbis_project_identifier = ?";
    String res = "";
    Connection conn = login();
    try {
      PreparedStatement statement = conn.prepareStatement(sql);
      statement.setString(1, projectIdentifier);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        res = rs.getString(1);
      }
    } catch (SQLException e) {
      LOG.error("SQL operation unsuccessful:", e);
    } catch (NullPointerException n) {
      LOG.error("Could not reach SQL database, resuming without project names.", n);
    } finally {
      logout(conn);
    }
    return res;
  }

  public int isProjectInDB(String projectIdentifier) {
    LOG.info("Looking for project {} in the DB", projectIdentifier);
    String sql = "SELECT * from projects WHERE openbis_project_identifier = ?";
    int res = -1;
    Connection conn = login();
    try {
      PreparedStatement statement = conn.prepareStatement(sql);
      statement.setString(1, projectIdentifier);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        res = rs.getInt("id");
        LOG.info("project found!");
      }
    } catch (SQLException e) {
      LOG.error("SQL operation unsuccessful", e);
    } finally {
      logout(conn);
    }
    return res;
  }

  public int addProjectToDB(String projectIdentifier, String projectName) {
    int exists = isProjectInDB(projectIdentifier);
    if (exists < 0) {
      LOG.info("Trying to add project {} to the person DB", projectIdentifier);
      String sql = "INSERT INTO projects (openbis_project_identifier, short_title) VALUES(?, ?)";
      Connection conn = login();
      try (PreparedStatement statement =
          conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        statement.setString(1, projectIdentifier);
        statement.setString(2, projectName);
        statement.execute();
        ResultSet rs = statement.getGeneratedKeys();
        if (rs.next()) {
          LOG.info("Successful.");
          return rs.getInt(1);
        }
      } catch (SQLException e) {
        LOG.error("SQL operation unsuccessful", e);
      } finally {
        logout(conn);
      }
      return -1;
    }
    return exists;
  }

  public boolean hasPersonRoleInProject(int personID, int projectID, String role) {
    LOG.info("Checking if person already has this role in the project.");
    String sql =
        "SELECT * from projects_persons WHERE person_id = ? AND project_id = ? and project_role = ?";
    boolean res = false;
    Connection conn = login();
    try {
      PreparedStatement statement = conn.prepareStatement(sql);
      statement.setInt(1, personID);
      statement.setInt(2, projectID);
      statement.setString(3, role);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        res = true;
        LOG.info("person already has this role!");
      }
    } catch (SQLException e) {
      LOG.error("SQL operation unsuccessful", e);
    } finally {
      logout(conn);
    }
    return res;
  }

  public void addPersonToProject(int projectID, int personID, String role) {
    if (!hasPersonRoleInProject(personID, projectID, role)) {
      LOG.info("Trying to add person with role {} to a project.", role);
      String sql =
          "INSERT INTO projects_persons (project_id, person_id, project_role) VALUES(?, ?, ?)";
      Connection conn = login();
      try (PreparedStatement statement =
          conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        statement.setInt(1, projectID);
        statement.setInt(2, personID);
        statement.setString(3, role);
        statement.execute();
        LOG.info("Successful.");
      } catch (SQLException e) {
        LOG.error("SQL operation unsuccessful: ", e);
      } finally {
        logout(conn);
      }
    }
  }

  /**
   * returns a map of principal investigator first+last names along with the pi_id. only returns
   * active investigators
   *
   * @return
   */
  public Map<String, Integer> getPrincipalInvestigatorsWithIDs() {
    String sql = "SELECT id, first_name, family_name FROM persons WHERE active = 1";
    Map<String, Integer> res = new HashMap<String, Integer>();
    Connection conn = login();
    try (PreparedStatement statement = conn.prepareStatement(sql)) {
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        int pi_id = rs.getInt("id");
        String first = rs.getString("first_name");
        String last = rs.getString("family_name");
        res.put(first + " " + last, pi_id);
      }
      statement.close();
    } catch (SQLException e) {
      LOG.error("Could not get PI", e);
    } finally {
      logout(conn);
    }

    return res;
  }

  public int addExperimentToDB(String id) {
    int exists = isExpInDB(id);
    if (exists < 0) {
      String sql = "INSERT INTO experiments (openbis_experiment_identifier) VALUES(?)";
      Connection conn = login();
      try (PreparedStatement statement =
          conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        statement.setString(1, id);
        statement.execute();
        ResultSet rs = statement.getGeneratedKeys();
        if (rs.next()) {
          return rs.getInt(1);
        }
      } catch (SQLException e) {
        LOG.error("Was trying to add experiment {} to the person DB", id);
        LOG.error("SQL operation unsuccessful", e);
      } finally {
        logout(conn);
      }
      return -1;
    }
    LOG.info("added experiment do mysql db");

    return exists;
  }

  private int isExpInDB(String id) {
    LOG.info("Looking for experiment " + id + " in the DB");
    String sql = "SELECT * from experiments WHERE openbis_experiment_identifier = ?";
    int res = -1;
    Connection conn = login();
    try {
      PreparedStatement statement = conn.prepareStatement(sql);
      statement.setString(1, id);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        LOG.info("experiment found!");
        res = rs.getInt("id");
      }
    } catch (SQLException e) {
      LOG.error("SQL operation unsuccessful", e);
    } finally {
      logout(conn);
    }

    return res;
  }

  public void addPersonToExperiment(int expID, int personID, String role) {
    if (expID == 0 || personID == 0)
      return;

    if (!hasPersonRoleInExperiment(personID, expID, role)) {
      LOG.info("Trying to add person with role {} to an experiment.", role);
      String sql =
          "INSERT INTO experiments_persons (experiment_id, person_id, experiment_role) VALUES(?, ?, ?)";
      Connection conn = login();
      try (PreparedStatement statement =
          conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        statement.setInt(1, expID);
        statement.setInt(2, personID);
        statement.setString(3, role);
        statement.execute();
        LOG.info("Successful.");
      } catch (SQLException e) {
        LOG.error("SQL operation unsuccessful ", e);
      } finally {
        logout(conn);
      }
    }
  }

  private boolean hasPersonRoleInExperiment(int personID, int expID, String role) {
    LOG.info("Checking if person already has this role in the experiment.");
    String sql =
        "SELECT * from experiments_persons WHERE person_id = ? AND experiment_id = ? and experiment_role = ?";
    boolean res = false;
    Connection conn = login();
    try {
      PreparedStatement statement = conn.prepareStatement(sql);
      statement.setInt(1, personID);
      statement.setInt(2, expID);
      statement.setString(3, role);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        res = true;
        LOG.info("person already has this role!");
      }
    } catch (SQLException e) {
      LOG.error("SQL operation unsuccessful", e);
    } finally {
      logout(conn);
    }

    return res;
  }

  // TODO unify to user group approach
  public Set<Printer> getPrintersForProject(String project, List<UserGroup> liferayUserGroupList) {
    Set<Printer> res = new HashSet<Printer>();
    // Printers associated with projects
    String sql =
        "SELECT projects.*, printer_project_association.*, labelprinter.* FROM projects, printer_project_association, labelprinter "
        + "WHERE projects.openbis_project_identifier LIKE ? "
        + "AND projects.id = printer_project_association.project_id "
        + "AND labelprinter.id = printer_project_association.printer_id";
    Connection conn = login();

    try (PreparedStatement statement = conn.prepareStatement(sql)){
      statement.setString(1, "%" + project);
      ResultSet rs = statement.executeQuery();

      while (rs.next()) {
        String location = rs.getString("location");
        String name = rs.getString("name");
        String ip = rs.getString("url");
        Printer.PrinterType type = Printer.PrinterType.fromString(rs.getString("type"));
        boolean adminOnly = rs.getBoolean("admin_only");
        String userGroup = rs.getString("user_group");
        // QBiC printer for admin users
        if (!adminOnly)
          res.add(new Printer(location, name, ip, type, adminOnly, userGroup));
      }
    } catch (Exception e) {
      LOG.error("Could not get printers", e);
    } finally {
      logout(conn);
    }

    // Printers associated with user groups
    sql = "SELECT * FROM labelprinter";
    conn = login();
    try (PreparedStatement statement = conn.prepareStatement(sql)){
      ResultSet rs = statement.executeQuery();

      while (rs.next()) {
        String location = rs.getString("location");
        String name = rs.getString("name");
        String ip = rs.getString("url");
        Printer.PrinterType type = Printer.PrinterType.fromString(rs.getString("type"));
        boolean adminOnly = rs.getBoolean("admin_only");
        String userGroup = rs.getString("user_group");
        if (adminOnly)
          res.add(new Printer(location, name, ip, type, adminOnly, userGroup));

        for (UserGroup ug : liferayUserGroupList) {
          if (ug.getName().equalsIgnoreCase(userGroup))
            res.add(new Printer(location, name, ip, type, adminOnly, userGroup));

        }
      }
    } catch (Exception e) {
      LOG.error("Could not get printers", e);
      res.add(new Printer("QBiC LAB", "TSC_TTP-343C", "printserv.qbic.uni-tuebingen.de",
          Printer.PrinterType.Label_Printer, true, ""));
    } finally {
      logout(conn);
    }
    LOG.debug("Found {}  printers for this user and project.", res.size());

    return res;
  }

  public Map<String, Integer> fetchPeople() {
    Map<String, Integer> map = new HashMap<String, Integer>();
    try {
      map = getPrincipalInvestigatorsWithIDs();
    } catch (NullPointerException e) {
      map.put("No Connection", -1);
    }

    return map;
  }


  public void addLabelCountEntry(String printerName, String printerLocation, String projectSpace,
      String userName, String subProject, int numLabels) {
    String selectPrinterID = getPrinterIDQuery(printerName, printerLocation);
    String selectProjectID = getProjektIDQuery(projectSpace, subProject);
    String sql;
    int old_num_printed = getNumExistingPrinted(selectPrinterID, selectProjectID, userName);
    if (old_num_printed > -1) {
      StringBuilder sb = new StringBuilder("UPDATE printed_label_counts SET num_printed = '");
      numLabels += old_num_printed;
      sb.append(Integer.toString(numLabels));
      sb.append("' WHERE printer_id = (");
      sb.append(selectPrinterID);
      sb.append(") AND project_id = (");
      sb.append(selectProjectID);
      sb.append(") AND user_name = '");
      sb.append(userName);
      sb.append("';");
      sql = sb.toString();
    } else {
      StringBuilder sb = new StringBuilder(
          "INSERT INTO printed_label_counts (printer_id, project_id, user_name, num_printed) VALUES ((");
      sb.append(selectPrinterID);
      sb.append("),(");
      sb.append(selectProjectID);
      sb.append("),'");
      sb.append(userName);
      sb.append("','");
      sb.append(Integer.toString(numLabels));
      sb.append("');");
      sql = sb.toString();
    }
    executeFreeQuery(sql);
  }

  private int getNumExistingPrinted(String selectPrinterID, String selectProjectID,
      String userName) {
    StringBuilder sb = new StringBuilder("SELECT * FROM printed_label_counts WHERE printer_id = (");
    sb.append(selectPrinterID);
    sb.append(") AND project_id = (");
    sb.append(selectProjectID);
    sb.append(") AND user_name = '");
    sb.append(userName);
    sb.append("';");

    try {
      SQLContainer s = loadTableFromQuery(sb.toString());
      if (s.getItemIds().size() > 0) {

        // This somehow works to access an entry, however the loop should have exactly one iteration
        for (Object itemId : s.getItemIds()) {
          Property property = s.getContainerProperty(itemId, "num_printed");
          Object data = property.getValue();
          return Integer.parseInt(data.toString());
        }
      }
    } catch (SQLException ignored) {

    }

    return -1;
  }

  public SQLContainer loadTableFromQuery(String query) throws SQLException {
    FreeformQuery freeformQuery = new FreeformQuery(query, connectionPool);
    return new SQLContainer(freeformQuery);
  }

  private String getPrinterIDQuery(String name, String location) {

    StringBuilder sb =
        new StringBuilder("SELECT labelprinter.id FROM labelprinter WHERE labelprinter.name = '");
    sb.append(name);
    sb.append("' AND labelprinter.location = '");
    sb.append(location);
    sb.append("'");

    return sb.toString();
  }

  private String getProjektIDQuery(String space, String subProject) {
    StringBuilder sb = new StringBuilder(
        "SELECT projects.id FROM projects WHERE projects.openbis_project_identifier = '/");
    sb.append(space);
    sb.append("/");
    sb.append(subProject);
    sb.append("'");

    return sb.toString();
  }

  private void executeFreeQuery(String query) {
    Connection conn = null;
    try {

      conn = login();
      Statement statement = conn.createStatement();

      statement.executeUpdate(query);
      statement.close();
      conn.setAutoCommit(true);
      conn.commit();
    } catch (SQLException ignored) {

    } finally {
      logout(conn);
    }
  }

}
