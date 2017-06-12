package life.qbic.barcoder;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;

import life.qbic.barcoder.control.BarcodeController;
import life.qbic.barcoder.control.SampleFilterGenerator;
import life.qbic.barcoder.io.BarcodeConfig;
import life.qbic.barcoder.io.ConfigurationManager;
import life.qbic.barcoder.io.ConfigurationManagerFactory;
import life.qbic.barcoder.io.DBConfig;
import life.qbic.barcoder.io.DBManager;
import life.qbic.barcoder.logging.Logger;
import life.qbic.openbis.openbisclient.IOpenBisClient;
import life.qbic.openbis.openbisclient.OpenBisClient;
import life.qbic.openbis.openbisclient.OpenBisClientMock;
import life.qbic.portal.liferayandvaadinhelpers.main.LiferayAndVaadinUtils;
import life.qbic.barcoder.logging.Log4j2Logger;
import life.qbic.barcoder.view.BarcodeView;

import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
@Theme("qbicbarcodedragon")
public class QbicbarcodedragonUI extends UI {

  @WebServlet(value = "/*", asyncSupported = true)
  @VaadinServletConfiguration(productionMode = false, ui = QbicbarcodedragonUI.class, widgetset = "life.qbic.barcoder.widgetset.QbicbarcodedragonWidgetset")
  public static class Servlet extends VaadinServlet {
  }

  Logger logger = new Log4j2Logger(QbicbarcodedragonUI.class);
  private String version = "Version 1.0, 26.01.17";
  private boolean isAdmin = false;
  private boolean testMode = false;
  public static String tmpFolder;

  private IOpenBisClient openbis;
  private BarcodeView mainView;
  private ConfigurationManager config;

  @Override
  protected void init(VaadinRequest request) {
    VerticalLayout layout = new VerticalLayout();

    // read in the configuration file
    config = ConfigurationManagerFactory.getInstance();
    tmpFolder = config.getTmpFolder();
    layout.setMargin(true);
    setContent(layout);
    String userID = "";
    boolean success = true;
    if (LiferayAndVaadinUtils.isLiferayPortlet()) {
      logger.info("Barcode Dragon is running on Liferay and user is logged in.");
      userID = LiferayAndVaadinUtils.getUser().getScreenName();
    } else {
      if (isDevelopment()) {
        logger.warn("Checks for local dev version successful. User is granted admin status.");
        userID = "admin";
        isAdmin = true;
      } else {
        success = false;
        logger.info(
            "User \"" + userID + "\" not found. Probably running on Liferay and not logged in.");
        layout.addComponent(new Label("User not found. Are you logged in?"));
      }
    }
    // establish connection to the OpenBIS API
    if (!isDevelopment() || !testMode) {
      try {
        logger.debug("trying to connect to openbis");
        this.openbis = new OpenBisClient(config.getDataSourceUser(), config.getDataSourcePassword(),
            config.getDataSourceUrl());
        this.openbis.login();
      } catch (Exception e) {
        success = false;
        logger.error(
            "User \"" + userID + "\" could not connect to openBIS and has been informed of this.");
        layout.addComponent(new Label(
            "Data Management System could not be reached. Please try again later or contact us."));
      }
    }
    if (isDevelopment() && testMode) {
      logger.error("No connection to openBIS. Trying mock version for testing.");
      this.openbis = new OpenBisClientMock(config.getDataSourceUser(),
          config.getDataSourcePassword(), config.getDataSourceUrl());
      layout.addComponent(new Label(
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
      layout.addComponent(mainView);
    }
    if (LiferayAndVaadinUtils.isLiferayPortlet())
      try {
        for (com.liferay.portal.model.Role r : LiferayAndVaadinUtils.getUser().getRoles())
          if (r.getName().equals("Administrator")) {// TODO what other roles?
            layout.addComponent(new Label(version));
            layout.addComponent(new Label("User: " + userID));
          }
      } catch (Exception e) {
        success = false;
        layout.addComponent(new Label("Unkown user. Are you logged in?"));
      }
    else {
      layout.addComponent(new Label(version));
      layout.addComponent(new Label("User: " + userID));
    }
  }

  boolean isDevelopment() {
    boolean devEnv = false;
    try {
      // TODO tests if this is somehow a local development environment
      // in which case user is granted admin rights. Change so it works for you.
      // Be careful that this is never true on production or better yet that logged out users can
      // not see the portlet page.
      String path = new File(".").getCanonicalPath();
      devEnv = path.toLowerCase().contains("eclipse");
    } catch (IOException e) {
      e.printStackTrace();
    }
    return devEnv;
  }

  private void initView(final DBManager dbm, List<String> spaces, final String user) {
    BarcodeConfig bcConf = new BarcodeConfig(config.getBarcodeScriptsFolder(), tmpFolder,
        config.getBarcodeResultsFolder(), config.getBarcodePathVariable());
    SampleFilterGenerator gen = new SampleFilterGenerator();
    BarcodeController bc = new BarcodeController(openbis, bcConf, dbm);
    gen.addObserver(bc);
    mainView = new BarcodeView(spaces, isAdmin, gen);
    mainView.setStyleName(ValoTheme.LAYOUT_WELL);
    mainView.initControl(bc);
    mainView.setIcon(FontAwesome.BARCODE);
    if (isAdmin) {
      logger.info("User is " + user + " and can print barcodes.");
    }
  }
}
