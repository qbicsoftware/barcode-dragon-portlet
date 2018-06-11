/*******************************************************************************
 * QBiC Project Wizard enables users to create hierarchical experiments including different study conditions using factorial design.
 * Copyright (C) "2016"  Andreas Friedrich
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package life.qbic.portal.portlet.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Sample;

import com.vaadin.ui.ComboBox;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import life.qbic.portal.portlet.util.SampleToBarcodeFieldTranslator;
import life.qbic.portal.portlet.util.Styles;

public class SheetOptionComponent extends VerticalLayout {

  private ComboBox firstOption;
  private ComboBox secondOption;

  private SampleToBarcodeFieldTranslator translator;

  private List<String> options = new ArrayList<String>(Arrays.asList("Lab ID",
      "Tissue/Extr. Material", "Secondary Name", "Parent Samples (Source)"));

  public SheetOptionComponent(SampleToBarcodeFieldTranslator translator) {
    this.translator = translator;

    setMargin(true);
    setSpacing(true);

    TextField firstCol = new TextField("First Column");
    firstCol.setStyleName(Styles.fieldTheme);
    firstCol.setValue("QBiC Barcode");
    firstCol.setEnabled(false);
    addComponent(Styles
        .questionize(
            firstCol,
            "Choose which columns will be in the spread sheet containing your samples and how they will be sorted. "
                + "The first column always contains a scannable barcode and the last is reserved for notes.",
            "Design your Sample Sheet"));
    firstOption = new ComboBox("Second Column", options);
    firstOption.setNullSelectionAllowed(false);
    firstOption.setStyleName(Styles.boxTheme);
    firstOption.setValue("Secondary Name");
    secondOption = new ComboBox("Third Column", options);
    secondOption.setNullSelectionAllowed(false);
    secondOption.setStyleName(Styles.boxTheme);
    secondOption.setValue("Parent Samples (Source)");
    addComponent(firstOption);
    addComponent(secondOption);
  }

  public String getInfo1(Sample s, String parents) {
    return translator.buildInfo(firstOption, s, parents, false);
  }

  public String getInfo2(Sample s, String parents) {
    return translator.buildInfo(secondOption, s, parents, false);
  }

  public List<String> getHeaders() {
    return new ArrayList<String>(Arrays.asList((String) firstOption.getValue(),
        (String) secondOption.getValue()));
  }


}
