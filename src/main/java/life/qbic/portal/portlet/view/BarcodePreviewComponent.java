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
package life.qbic.portal.portlet.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Sample;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.Resource;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import life.qbic.portal.portlet.util.SampleToBarcodeFieldTranslator;
import life.qbic.portal.portlet.util.Styles;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BarcodePreviewComponent extends VerticalLayout {

  private static final Logger LOG = LogManager.getLogger(BarcodePreviewComponent.class);

  /**
   * 
   */
  private static final long serialVersionUID = -1785004617342053619L;
  TextField code;
  TextField info1;
  TextField info2;
  TextField person;
  TextField qbicInfo = new TextField("", "www.qbic.life");
  // qbicInfo = new TextField("", "QBiC: +4970712972163");
  private OptionGroup codedName;
  private ComboBox select1;
  private ComboBox select2;
//  private CheckBox overwrite;
  // private TextField codedNameField;

  Sample example;
  SampleToBarcodeFieldTranslator translator;

  public BarcodePreviewComponent(SampleToBarcodeFieldTranslator translator) {
    this.translator = translator;
    setSpacing(true);
    setMargin(true);

    Resource res = new ThemeResource("img/qrtest.png");
    Image qr = new Image(null, res);
    qr.setHeight("140px");
    qr.setWidth("140px");
    Image qr2 = new Image(null, res);
    qr2.setHeight("140px");
    qr2.setWidth("140px");

    code = new TextField();
    info1 = new TextField();
    info2 = new TextField();

    codedName = new OptionGroup("Put ID on sticker:");
    codedName.addItems(Arrays.asList("QBiC ID", "Lab ID", "Secondary Name"));
    codedName.setImmediate(true);
    codedName.addStyleName(ValoTheme.OPTIONGROUP_HORIZONTAL);
    codedName.select("QBiC ID");

    code.addStyleName(ValoTheme.TEXTFIELD_BORDERLESS);
    code.setWidth("200px");
    code.addStyleName("barcode-large");

    styleInfoField(info1);
    styleInfoField(info2);
    styleInfoField(qbicInfo);

    VerticalLayout box = new VerticalLayout();
    box.setHeight("110px");
    box.addStyleName(ValoTheme.LAYOUT_COMPONENT_GROUP);
    box.addComponent(code);
    box.addComponent(info1);
    box.addComponent(info2);
    box.addComponent(qbicInfo);
    box.setWidth("190px");

    HorizontalLayout test = new HorizontalLayout();
    test.setSizeFull();
    test.addComponent(qr);
    test.addComponent(box);
    test.addComponent(qr2);

    setFieldsReadOnly(true);
    List<String> options =
        new ArrayList<String>(Arrays.asList("Tissue/Extr. Material", "Secondary Name", "QBiC ID",
            "Lab ID", "MHC Type", "Used Antibody"));
    select1 = new ComboBox("First Info", options);
    select1.setStyleName(Styles.boxTheme);
    select1.setImmediate(true);
    select1.select("Tissue/Extr. Material");
    select2 = new ComboBox("Second Info", options);
    select2.select("Secondary Name");
    select2.setImmediate(true);
    select2.setStyleName(Styles.boxTheme);

    ValueChangeListener vc = new ValueChangeListener() {

      /**
       * 
       */
      private static final long serialVersionUID = -7466519211904860012L;

      @Override
      public void valueChange(ValueChangeEvent event) {
        refresh();
      }
    };
    codedName.addValueChangeListener(vc);
    select1.addValueChangeListener(vc);
    select2.addValueChangeListener(vc);

    HorizontalLayout designBox = new HorizontalLayout();
    designBox.addComponent(select1);
    designBox.addComponent(select2);
    designBox.addStyleName(ValoTheme.LAYOUT_HORIZONTAL_WRAPPING);
    designBox.setSpacing(true);

    VerticalLayout previewBox = new VerticalLayout();
    previewBox.setStyleName(ValoTheme.LAYOUT_CARD);
    previewBox.setCaption("Barcode Example");
    previewBox.addComponent(test);

    addComponent(previewBox);
    addComponent(codedName);
    addComponent(designBox);

//    overwrite = new CheckBox("Overwrite existing Tube Barcode Files");
//    addComponent(ProjectwizardUI.questionize(overwrite,
//        "Overwrites existing files of barcode stickers. This is useful when "
//            + "the design was changed after creating them.", "Overwrite Sticker Files"));
  }

  private void styleInfoField(TextField tf) {
    tf.addStyleName(ValoTheme.TEXTFIELD_BORDERLESS);
    tf.setWidth("200px");
    tf.addStyleName("barcode-preview");
  }

  public void update(String s1, String s2) {
    info1.setValue(s1);
    info2.setValue(s2);
  }

  private void setFieldsReadOnly(boolean b) {
    qbicInfo.setReadOnly(b);
    code.setReadOnly(b);
    info1.setReadOnly(b);
    info2.setReadOnly(b);
  }

  public void setExample(Sample sample) {
    example = sample;
    refresh();
  }

  public void refresh() {
    setFieldsReadOnly(false);
    code.setValue(example.getCode());
    code.setValue(getCodeString(example));
    info1.setValue(getInfo(select1, example));
    info2.setValue(getInfo(select2, example));
    setFieldsReadOnly(true);
  }

  public String getCodeString(Sample s) {
    return translator.getCodeString(s, (String) codedName.getValue());
  }

  public String getInfo(ComboBox b, Sample s) {
    return translator.buildInfo(b, s, null, true);
  }

  public String getInfo1(Sample s) {
    return getInfo(select1, s);
  }

  public String getInfo2(Sample s) {
    return getInfo(select2, s);
  }

//  public boolean overwrite() {
//    return overwrite.getValue();
//  }
}
