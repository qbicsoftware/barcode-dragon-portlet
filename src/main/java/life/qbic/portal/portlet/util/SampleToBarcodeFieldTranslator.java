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
package life.qbic.portal.portlet.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Sample;
import life.qbic.portal.portlet.model.IBarcodeOptions;
import life.qbic.portal.portlet.model.QRInfoOptions;
import life.qbic.portal.portlet.model.SheetInfoOptions;

import com.vaadin.ui.ComboBox;

public class SampleToBarcodeFieldTranslator {

  private static final Logger LOG = LogManager.getLogger(SampleToBarcodeFieldTranslator.class);
  private final int HEADER_MAX_LENTH = 15; // cutoff value of the ID line printed on tube barcode
                                           // stickers
  private final int INFO_MAX_LENGTH = 21; // cutoff value of the two description lines printed on
                                          // tube barcode stickers

  public String buildInfo(ComboBox select, Sample s, String parents, boolean cut) {
    Map<String, String> map = s.getProperties();
    IBarcodeOptions option = null;
    if (select.getValue() != null) {
      String val = select.getValue().toString();
      option = QRInfoOptions.fromString(val);
      if (option == null) {
        option = SheetInfoOptions.fromString(val);
      }
    }

    Map<IBarcodeOptions, String> translator = new HashMap<IBarcodeOptions, String>() {
      /**
       * 
       */
      private static final long serialVersionUID = -5308120857192708672L;

      {
        put(QRInfoOptions.Extract_Material, getMaterial(map));
        put(QRInfoOptions.MHC_Type, map.get("Q_MHC_CLASS"));
        put(QRInfoOptions.Antibody, map.get("Q_ANTIBODY"));
        put(SheetInfoOptions.Parent_Samples, parents);
        put(QRInfoOptions.Secondary_Name, map.get("Q_SECONDARY_NAME"));
        put(QRInfoOptions.QBIC_Code, s.getCode());
        put(QRInfoOptions.Lab_ID, map.get("Q_EXTERNALDB_ID"));
      }

      private String getMaterial(Map<String, String> map) {
        if (map.containsKey("Q_PRIMARY_TISSUE"))
          return map.get("Q_PRIMARY_TISSUE");
        else if (map.containsKey("Q_MHC_CLASS"))
          return map.get("Q_MHC_CLASS");
        else
          return map.get("Q_SAMPLE_TYPE");
      }
    };
    String res = translator.get(option);
    if (res == null)
      return "";
    if (cut)
      res = res.substring(0, Math.min(res.length(), INFO_MAX_LENGTH));
    return res;
  }

  public String getCodeString(Sample sample, String codedName) {
    Map<String, String> map = sample.getProperties();
    String res = "";
    String s = codedName;
    if (!res.isEmpty())
      res += "_";
    switch (s) {
      case "QBiC ID":
        res += sample.getCode();
        break;
      case "Secondary Name":
        res += map.get("Q_SECONDARY_NAME");
        break;
      case "Lab ID":
        res += map.get("Q_EXTERNALDB_ID");
        break;
    }
    res = fixFileName(res);
    return res.substring(0, Math.min(res.length(), HEADER_MAX_LENTH));
  }

  private String fixFileName(String res) {
    res = res.replace("null", "");
    res = res.replace(";", "_");
    res = res.replace("#", "_");
    res = res.replace(" ", "_");
    while (res.contains("__"))
      res = res.replace("__", "_");
    return res;
  }
}
