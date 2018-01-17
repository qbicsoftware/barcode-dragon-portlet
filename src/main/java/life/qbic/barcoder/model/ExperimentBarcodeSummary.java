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
package life.qbic.barcoder.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Sample;

/**
 * Bean Object representing an experiment with some information about its samples to provide an
 * overview, e.g. for barcode creation
 * 
 * @author Andreas Friedrich
 * 
 */
public class ExperimentBarcodeSummary {

  private String bioType;
  private String amount;
  private String experimentID;
  private String date;
  private List<Sample> samples;

  /**
   * Creates a new ExperimentBarcodeSummaryBean
   * 
   * @param bioType the type of samples in this experiment, for example tissue or measurement type
   * @param amount the amount of samples in this experiment
   */
  public ExperimentBarcodeSummary(String bioType, String amount, String expID, String date) {
    this.bioType = bioType;
    this.amount = amount;
    this.experimentID = expID;
    this.date = date;
    this.samples = new ArrayList<Sample>();
  }

  // show only code
  public String getExperiment() {
    String[] split = experimentID.split("/");
    return split[split.length - 1];
  }

  public String getDate() {
    return date;
  }

  public String fetchExperimentID() {
    return experimentID;
  }

  public String getBio_Type() {
    return bioType;
  }

  public void setBio_Type(String bio_Type) {
    this.bioType = bio_Type;
  }

  public void increment() {
    this.amount = Integer.toString(Integer.parseInt(amount) + 1);
  }

  public String getAmount() {
    return amount;
  }

  public void setAmount(String amount) {
    this.amount = amount;
  }

  public void addSample(Sample s) {
    this.samples.add(s);
  }

  public List<Sample> getSamples() {
    return samples;
  }

}
