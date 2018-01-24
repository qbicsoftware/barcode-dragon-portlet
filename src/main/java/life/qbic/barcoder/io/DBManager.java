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
package life.qbic.barcoder.io;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

import com.liferay.portal.model.UserGroup;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.sqlcontainer.SQLContainer;
import com.vaadin.data.util.sqlcontainer.connection.JDBCConnectionPool;
import com.vaadin.data.util.sqlcontainer.connection.SimpleJDBCConnectionPool;
import com.vaadin.data.util.sqlcontainer.query.FreeformQuery;
import life.qbic.barcoder.helpers.Styles;
import life.qbic.barcoder.logging.Log4j2Logger;
import life.qbic.barcoder.logging.Logger;
import life.qbic.barcoder.model.Person;
import life.qbic.barcoder.model.Printer;
import life.qbic.barcoder.model.Printer.PrinterType;
import life.qbic.portal.liferayandvaadinhelpers.main.LiferayAndVaadinUtils;

public class DBManager {

    private DBConfig config;

    Logger logger = new Log4j2Logger(DBManager.class);

    public DBManager(DBConfig config) {
        this.config = config;
    }

    private void logout(Connection conn) {
        try {
            if (conn != null)
                conn.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private Connection login() {
        String DB_URL = "jdbc:mariadb://" + config.getHostname() + ":" + config.getPort() + "/"
                + config.getSql_database();

        Connection conn = null;

        try {
            Class.forName("org.mariadb.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, config.getUsername(), config.getPassword());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return conn;
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
                String zdvID = rs.getString("username");
                String first = rs.getString("first_name");
                String last = rs.getString("family_name");
                String email = rs.getString("email");
                String tel = rs.getString("phone");
                int instituteID = -1;// TODO fetch correct id
                res = new Person(zdvID, first, last, email, tel, instituteID);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            logout(conn);
            // LOGGER.debug("Project not associated with Investigator. PI will be set to 'Unknown'");
        }

        logout(conn);
        return res;
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
            logger.error("SQL operation unsuccessful: " + e.getMessage());
            e.printStackTrace();
        } catch (NullPointerException n) {
            logger.error("Could not reach SQL database, resuming without project names.");
        }
        logout(conn);
        return res;
    }

    public int isProjectInDB(String projectIdentifier) {
        logger.info("Looking for project " + projectIdentifier + " in the DB");
        String sql = "SELECT * from projects WHERE openbis_project_identifier = ?";
        int res = -1;
        Connection conn = login();
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, projectIdentifier);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                res = rs.getInt("id");
                logger.info("project found!");
            }
        } catch (SQLException e) {
            logger.error("SQL operation unsuccessful: " + e.getMessage());
            e.printStackTrace();
        }
        logout(conn);
        return res;
    }

    public int addProjectToDB(String projectIdentifier, String projectName) {
        int exists = isProjectInDB(projectIdentifier);
        if (exists < 0) {
            logger.info("Trying to add project " + projectIdentifier + " to the person DB");
            String sql = "INSERT INTO projects (openbis_project_identifier, short_title) VALUES(?, ?)";
            Connection conn = login();
            try (PreparedStatement statement =
                         conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, projectIdentifier);
                statement.setString(2, projectName);
                statement.execute();
                ResultSet rs = statement.getGeneratedKeys();
                if (rs.next()) {
                    logout(conn);
                    logger.info("Successful.");
                    return rs.getInt(1);
                }
            } catch (SQLException e) {
                logger.error("SQL operation unsuccessful: " + e.getMessage());
                e.printStackTrace();
            }
            logout(conn);
            return -1;
        }
        return exists;
    }

    public boolean hasPersonRoleInProject(int personID, int projectID, String role) {
        logger.info("Checking if person already has this role in the project.");
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
                logger.info("person already has this role!");
            }
        } catch (SQLException e) {
            logger.error("SQL operation unsuccessful: " + e.getMessage());
            e.printStackTrace();
        }
        logout(conn);
        return res;
    }

    public void addPersonToProject(int projectID, int personID, String role) {
        if (!hasPersonRoleInProject(personID, projectID, role)) {
            logger.info("Trying to add person with role " + role + " to a project.");
            String sql =
                    "INSERT INTO projects_persons (project_id, person_id, project_role) VALUES(?, ?, ?)";
            Connection conn = login();
            try (PreparedStatement statement =
                         conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, projectID);
                statement.setInt(2, personID);
                statement.setString(3, role);
                statement.execute();
                logger.info("Successful.");
            } catch (SQLException e) {
                logger.error("SQL operation unsuccessful: " + e.getMessage());
                e.printStackTrace();
            }
            logout(conn);
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
            e.printStackTrace();
        }
        logout(conn);
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
                    logout(conn);
                    return rs.getInt(1);
                }
            } catch (SQLException e) {
                logger.error("Was trying to add experiment " + id + " to the person DB");
                logger.error("SQL operation unsuccessful: " + e.getMessage());
            }
            logout(conn);
            return -1;
        }
        logger.info("added experiment do mysql db");
        return exists;
    }

    private int isExpInDB(String id) {
        logger.info("Looking for experiment " + id + " in the DB");
        String sql = "SELECT * from experiments WHERE openbis_experiment_identifier = ?";
        int res = -1;
        Connection conn = login();
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, id);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                logger.info("experiment found!");
                res = rs.getInt("id");
            }
        } catch (SQLException e) {
            logger.error("SQL operation unsuccessful: " + e.getMessage());
            e.printStackTrace();
        }
        logout(conn);
        return res;
    }

    public void addPersonToExperiment(int expID, int personID, String role) {
        if (expID == 0 || personID == 0)
            return;

        if (!hasPersonRoleInExperiment(personID, expID, role)) {
            logger.info("Trying to add person with role " + role + " to an experiment.");
            String sql =
                    "INSERT INTO experiments_persons (experiment_id, person_id, experiment_role) VALUES(?, ?, ?)";
            Connection conn = login();
            try (PreparedStatement statement =
                         conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, expID);
                statement.setInt(2, personID);
                statement.setString(3, role);
                statement.execute();
                logger.info("Successful.");
            } catch (SQLException e) {
                logger.error("SQL operation unsuccessful: " + e.getMessage());
                e.printStackTrace();
            }
            logout(conn);
        }
    }

    private boolean hasPersonRoleInExperiment(int personID, int expID, String role) {
        logger.info("Checking if person already has this role in the experiment.");
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
                logger.info("person already has this role!");
            }
        } catch (SQLException e) {
            logger.error("SQL operation unsuccessful: " + e.getMessage());
            e.printStackTrace();
        }
        logout(conn);
        return res;
    }

    private void endQuery(Connection c, PreparedStatement p) {
        if (p != null)
            try {
                p.close();
            } catch (Exception e) {
                logger.error("PreparedStatement close problem");
            }
        if (c != null)
            try {
                logout(c);
            } catch (Exception e) {
                logger.error("Database Connection close problem");
            }
    }

    // TODO test this once tables exist
    public Set<Printer> getPrintersForProject(String project, List<UserGroup> liferayUserGroupList) {
        Set<Printer> res = new HashSet<Printer>();
        String sql =
                "SELECT projects.*, printer_project_association.*, labelprinter.* FROM projects, printer_project_association, labelprinter WHERE projects.openbis_project_identifier LIKE ?"
                        + " AND projects.id = printer_project_association.project_id";
        Connection conn = login();
        PreparedStatement statement = null;
        try {
            statement = conn.prepareStatement(sql);
            statement.setString(1, "%" + project);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                String location = rs.getString("location");
                String name = rs.getString("name");
                String ip = rs.getString("url");
                PrinterType type = PrinterType.fromString(rs.getString("type"));
                boolean adminOnly = rs.getBoolean("admin_only");
                String userGroup = rs.getString("user_group");
                if (!adminOnly)
                    res.add(new Printer(location, name, ip, type, adminOnly, userGroup));
            }
        } catch (Exception e) {
            e.printStackTrace();
            logout(conn);
        } finally {
            endQuery(conn, statement);
        }

        sql = "SELECT * FROM labelprinter";
        conn = login();
        statement = null;
        try {
            statement = conn.prepareStatement(sql);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                String location = rs.getString("location");
                String name = rs.getString("name");
                String ip = rs.getString("url");
                PrinterType type = PrinterType.fromString(rs.getString("type"));
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
            e.printStackTrace();
            logout(conn);
            res.add(new Printer("QBiC LAB", "TSC_TTP-343C", "printserv.qbic.uni-tuebingen.de",
                    PrinterType.Label_Printer, true, ""));
        } finally {
            endQuery(conn, statement);
        }
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


    public void addLabelCountEntry(String printerName, String printerLocation, String projectSpace, String userName, String subProject, String numLabels){
        String selectPrinterID = getPrinterIDQuery(printerName, printerLocation);
        String selectProjectID = getProjektIDQuery(projectSpace, subProject);
        //TODO check if entry already exists: in that case count up
        String sql;
        if(hasEntry(selectPrinterID,selectProjectID, userName)){
            //TODO this has not been tested: if this works then figure out how to add values
            StringBuilder sb = new StringBuilder("UPDATE printed_label_counts SET num_printed = '");
            sb.append((numLabels + 100));
            sb.append("' WHERE printer_id = (");
            sb.append(selectPrinterID);
            sb.append(") AND project_id = (");
            sb.append(selectProjectID);
            sb.append(") AND user_name = '");
            sb.append(userName);
            sb.append("';");
            sql = sb.toString();
        }else {

            StringBuilder sb = new StringBuilder("INSERT INTO printed_label_counts (printer_id, project_id, user_name, num_printed) VALUES ((");
            sb.append(selectPrinterID);
            sb.append("),(");
            sb.append(selectProjectID);
            sb.append("),'");
            sb.append(userName);
            sb.append("','");
            sb.append(numLabels);
            sb.append("');");
            sql = sb.toString();
        }
        executeFreeQuery(sql);



    }

    private boolean hasEntry(String selectPrinterID, String selectProjectID, String userName){


        //TODO continue here: this goes wrong somehow: seems ok now
        StringBuilder sb = new StringBuilder("SELECT * FROM printed_label_counts WHERE printer_id = (");
        sb.append(selectPrinterID);
        sb.append(") AND project_id = (");
        sb.append(selectProjectID);
        sb.append(") AND user_name = '");
        sb.append(userName);
        sb.append("';");
        Styles.notification("Information", sb.toString(),
                Styles.NotificationType.ERROR);
        try {
            SQLContainer s = loadTableFromQuery(sb.toString());
            if(s.getItemIds().size() > 0){

                Item item = s.getItem(s.getIdByIndex(0));

                Property property= s.getContainerProperty(s.getIdByIndex(0), "num_printed");
                Object data= property.getValue();

                Styles.notification("Information", "Entry exists " + data.toString(),
                        Styles.NotificationType.ERROR);
                return true;
            }
        }catch(SQLException e){

        }
        Styles.notification("Information", "Entry does not exist",
                Styles.NotificationType.ERROR);
        return false;
    }

    public SQLContainer loadTableFromQuery(String query) throws SQLException{
        JDBCConnectionPool pool = new SimpleJDBCConnectionPool("com.mysql.jdbc.Driver", "jdbc:mariadb://" + config.getHostname()+ ":" + config.getPort() + "/" + config.getSql_database(), config.getUsername(), config.getPassword(), 5, 10);

        FreeformQuery freeformQuery = new FreeformQuery(query,pool);
        return new SQLContainer(freeformQuery);
    }

    private String getPrinterIDQuery(String name, String location){

        StringBuilder sb = new StringBuilder("SELECT labelprinter.id FROM labelprinter WHERE labelprinter.name = '");
        sb.append(name);
        sb.append("' AND labelprinter.location = '");
        sb.append(location);
        sb.append("'");

        return sb.toString();
    }

    private String getProjektIDQuery(String space, String subProject){
        StringBuilder sb = new StringBuilder("SELECT projects.id FROM projects WHERE projects.openbis_project_identifier = '/");
        sb.append(space);
        sb.append("/");
        sb.append(subProject);
        sb.append("'");

        return sb.toString();
    }

    private void executeFreeQuery(String query){
        Connection conn = null;
        try {

            conn = login();
            Statement statement = conn.createStatement();

            statement.executeUpdate(query);
            statement.close();
            conn.setAutoCommit(true);
            conn.commit();
        } catch (SQLException e) {

        } finally {
            logout(conn);
        }
    }


    //
    // public String getInvestigatorForProject(String projectCode) {
    // String id_query = "SELECT pi_id FROM projects WHERE project_code = " + projectCode;
    // String id = "";
    // Connection conn = login();
    // try (PreparedStatement statement = conn.prepareStatement(id_query)) {
    // ResultSet rs = statement.executeQuery();
    // while (rs.next()) {
    // id = Integer.toString(rs.getInt("pi_id"));
    // }
    // statement.close();
    // } catch (SQLException e) {
    // e.printStackTrace();
    // }
    //
    // String sql = "SELECT first_name, last_name FROM project_investigators WHERE pi_id = " + id;
    // String fullName = "";
    // try (PreparedStatement statement = conn.prepareStatement(sql)) {
    // ResultSet rs = statement.executeQuery();
    // while (rs.next()) {
    // String first = rs.getString("first_name");
    // String last = rs.getString("last_name");
    // fullName = first + " " + last;
    // }
    // statement.close();
    // } catch (SQLException e) {
    // e.printStackTrace();
    // }
    // logout(conn);
    // return fullName;
    // }

    // /**
    // * add a new institute to the database. not in use yet since the schema is old
    // *
    // * @param name
    // * @param street
    // * @param zip
    // * @param city
    // * @return
    // */
    // public int addNewInstitute(String name, String street, String zip, String city) {
    // String sql = "insert into institutes (name, street, zip_code, city) " + "VALUES(?, ?, ?, ?)";
    // Connection conn = login();
    // try (PreparedStatement statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS))
    // {
    // statement.setString(1, name);
    // statement.setString(2, street);
    // statement.setString(3, zip);
    // statement.setString(4, city);
    // statement.execute();
    // ResultSet rs = statement.getGeneratedKeys();
    // if (rs.next()) {
    // return rs.getInt(1);
    // }
    // logger.info("Successful.");
    // } catch (SQLException e) {
    // logger.error("SQL operation unsuccessful: " + e.getMessage());
    // e.printStackTrace();
    // }
    // logout(conn);
    // return -1;
    // }

    // /**
    // * add a person whose institude id is known. not in use yet since the schema is old
    // *
    // * @return
    // */
    // public int addNewPersonWithInstituteID(Person p) {
    // String sql =
    // "insert into project_investigators (zdvID, first_name, last_name, email, phone, institute_id,
    // active) "
    // + "VALUES(?, ?, ?, ?, ?, ?, ?)";
    // Connection conn = login();
    // try (PreparedStatement statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS))
    // {
    // statement.setString(1, p.getZdvID());
    // statement.setString(2, p.getFirstName());
    // statement.setString(3, p.getLastName());
    // statement.setString(4, p.getEmail());
    // statement.setString(5, p.getPhone());
    // statement.setInt(6, p.getInstituteID());
    // statement.setInt(7, 1);
    // statement.execute();
    // ResultSet rs = statement.getGeneratedKeys();
    // if (rs.next()) {
    // return rs.getInt(1);
    // }
    // logger.info("Successful.");
    // } catch (SQLException e) {
    // logger.error("SQL operation unsuccessful: " + e.getMessage());
    // e.printStackTrace();
    // }
    // logout(conn);
    // return -1;
    // }

    // public int addNewPerson(PersonWithAdress p) {
    // String sql =
    // "insert into project_investigators (zdvID, first_name, last_name, email, street, zip_code,
    // city, phone, institute, active) "
    // + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    // Connection conn = login();
    // try (PreparedStatement statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS))
    // {
    // statement.setString(1, p.getZdvID());
    // statement.setString(2, p.getFirstName());
    // statement.setString(3, p.getLastName());
    // statement.setString(4, p.getEmail());
    // statement.setString(5, p.getStreet());
    // statement.setString(6, p.getZipCode());
    // statement.setString(7, p.getCity());
    // statement.setString(8, p.getPhone());
    // statement.setString(9, p.getInstitute());
    // statement.setInt(10, 1);
    // statement.execute();
    // ResultSet rs = statement.getGeneratedKeys();
    // if (rs.next()) {
    // return rs.getInt(1);
    // }
    // logger.info("Successful.");
    // } catch (SQLException e) {
    // logger.error("SQL operation unsuccessful: " + e.getMessage());
    // e.printStackTrace();
    // }
    // logout(conn);
    // return -1;
    // }

    // public void printPeople() {
    // String sql = "SELECT * FROM project_investigators";
    // Connection conn = login();
    // try (PreparedStatement statement = conn.prepareStatement(sql)) {
    // ResultSet rs = statement.executeQuery();
    // while (rs.next()) {
    // System.out.println(Integer.toString(rs.getInt(1)) + " " + rs.getString(2) + " "
    // + rs.getString(3) + " " + rs.getString(4) + " " + rs.getString(5) + " "
    // + rs.getString(6) + " " + rs.getString(7) + " " + rs.getString(8) + " "
    // + rs.getString(9) + " " + rs.getString(10) + " " + rs.getString(11));
    // }
    // statement.close();
    // } catch (SQLException e) {
    // e.printStackTrace();
    // }
    // }
    //
    // public void printProjects() {
    // String sql = "SELECT pi_id, project_code FROM projects";
    // Connection conn = login();
    // try (PreparedStatement statement = conn.prepareStatement(sql)) {
    // ResultSet rs = statement.executeQuery();
    // while (rs.next()) {
    // int pi_id = rs.getInt("pi_id");
    // String first = rs.getString("project_code");
    // System.out.println(pi_id + first);
    // }
    // statement.close();
    // } catch (SQLException e) {
    // e.printStackTrace();
    // }
    // }

}
