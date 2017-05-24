package com.qbic.life.portal_template;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Widgetset;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

@Theme("mytheme")
@SuppressWarnings("serial")
@Widgetset("com.qbic.life.portal_template.AppWidgetSet")
public class MyPortletUI extends UI {
  @Override
  protected void init(VaadinRequest request) {
    final VerticalLayout layout = new VerticalLayout();
    layout.setMargin(true);
    setContent(layout);

    final Label label = new Label(
        "This is a template for a qbic portlet. "
        + "It provides the correct maven structure for a liferay portlet.");
    layout.addComponent(label);
  }

}
