package life.qbic.portal.portlet;

import com.liferay.portal.model.UserGroup;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import life.qbic.openbis.openbisclient.IOpenBisClient;
import life.qbic.openbis.openbisclient.OpenBisClient;
import life.qbic.openbis.openbisclient.OpenBisClientMock;
import life.qbic.portal.liferayandvaadinhelpers.main.LiferayAndVaadinUtils;
import life.qbic.portal.portlet.control.BarcodeController;
import life.qbic.portal.portlet.control.SampleFilterGenerator;
import life.qbic.portal.portlet.io.*;
import life.qbic.portal.portlet.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Widgetset;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Layout;

import javax.servlet.annotation.WebServlet;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Entry point for portlet barcoder. This class derives from {@link QBiCPortletUI}, which is found in the {@code portal-utils-lib} library.
 */
@Theme("mytheme")
@SuppressWarnings("serial")
@Widgetset("life.qbic.portlet.AppWidgetSet")
public class BarcodeDragonUI extends QBiCPortletUI {

    private static final Logger LOG = LogManager.getLogger(BarcodeDragonUI.class);

    private boolean isAdmin = false;
    private boolean testMode = false;
    public static boolean development = true;
    public static String tmpFolder;

    private IOpenBisClient openbis;
    private BarcodeView mainView;
    private ConfigurationManager config;

    @Override
    protected Layout getPortletContent(final VaadinRequest request) {
        return createBarcodeDragonLayout(request);
    }

    private Layout createBarcodeDragonLayout(VaadinRequest request) {
        VerticalLayout mainLayout = new VerticalLayout();

        // read in the configuration file
        config = ConfigurationManagerFactory.getInstance();
        tmpFolder = config.getTmpFolder();
        mainLayout.setMargin(true);
        setContent(mainLayout);
        String userID = "";
        boolean success = true;
        if (LiferayAndVaadinUtils.isLiferayPortlet()) {
            LOG.info("Barcode Dragon is running on Liferay and user is logged in.");
            userID = LiferayAndVaadinUtils.getUser().getScreenName();
        } else {
            if (development) {
                LOG.warn("Checks for local dev version successful. User is granted admin status.");
                userID = "admin";
                isAdmin = true;
            } else {
                success = false;
                LOG.info(
                        "User \"" + userID + "\" not found. Probably running on Liferay and not logged in.");
                mainLayout.addComponent(new Label("User not found. Are you logged in?"));
            }
        }
        // establish connection to the OpenBIS API
        if (!testMode) {
            try {//
                LOG.debug("trying to connect to openbis");
                System.out.println(config.getDataSourceUser());
                this.openbis = new OpenBisClient(config.getDataSourceUser(), config.getDataSourcePassword(),
                        config.getDataSourceUrl());
                this.openbis.login();
            } catch (Exception e) {
                success = false;
                LOG.error(
                        "User \"" + userID + "\" could not connect to openBIS and has been informed of this.");
                mainLayout.addComponent(new Label(
                        "Data Management System could not be reached. Please try again later or contact us."));
            }
        }
        if (development && testMode) {
            LOG.error("No connection to openBIS. Trying mock version for testing.");
            this.openbis = new OpenBisClientMock(config.getDataSourceUser(),
                    config.getDataSourcePassword(), config.getDataSourceUrl());
            mainLayout.addComponent(new Label(
                    "openBIS could not be reached. Resuming with mock version. Some options might be non-functional. Reload to retry."));
        }
        if (success) {
            // stuff from openbis
            final List<String> spaces = openbis.getUserSpaces(userID);
            isAdmin = openbis.isUserAdmin(userID);
            // stuff from mysql database
            DBConfig mysqlConfig = new DBConfig(config.getMysqlHost(), config.getMysqlPort(),
                    config.getMysqlDB(), config.getMysqlUser(), config.getMysqlPass());
            DBManager dbm = new DBManager(mysqlConfig);
            // initialize the View with sample types, spaces and the dictionaries of tissues and species
            initView(dbm, spaces, userID);
            mainLayout.addComponent(mainView);
        }
        if (LiferayAndVaadinUtils.isLiferayPortlet())
            try {
                for (com.liferay.portal.model.Role r : LiferayAndVaadinUtils.getUser().getRoles())
                    if (r.getName().equals("Administrator")) {// TODO what other roles?
                        mainLayout.addComponent(new Label("User: " + userID));
                    }
            } catch (Exception e) {
                success = false;
                mainLayout.addComponent(new Label("Unkown user. Are you logged in?"));
            }
        else {
            mainLayout.addComponent(new Label("User: " + userID));
        }

        return mainLayout;
    }

    private void initView(final DBManager dbm, List<String> spaces, final String user) {
        BarcodeConfig bcConf = new BarcodeConfig(config.getBarcodeScriptsFolder(), tmpFolder,
                config.getBarcodeResultsFolder(), config.getBarcodePathVariable());
        SampleFilterGenerator gen = new SampleFilterGenerator();
        List<UserGroup> userGroupList = new ArrayList<>();
        try{
            userGroupList = LiferayAndVaadinUtils.getUser().getUserGroups();
        } catch (NullPointerException e) {
            LOG.error("Could not acquire user groups from user, are you testing outside liferay?");
        } catch (Exception exc){
            LOG.error("Could not acquire user groups from user.", exc);
        }
        BarcodeController bc = new BarcodeController(openbis, bcConf, dbm, userGroupList, user);
        gen.addObserver(bc);
        mainView = new BarcodeView(spaces, isAdmin, gen);
        mainView.setStyleName(ValoTheme.LAYOUT_WELL);
        mainView.initControl(bc);
        mainView.setIcon(FontAwesome.BARCODE);
        if (isAdmin) {
            LOG.info("User is " + user + " and can print barcodes.");
        }
    }


}