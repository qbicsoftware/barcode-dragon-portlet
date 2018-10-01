package life.qbic.portal.portlet.control;

import java.util.Observable;

import org.tepi.filtertable.FilterGenerator;

import com.vaadin.data.Container.Filter;
import com.vaadin.data.util.filter.Compare;
import com.vaadin.data.util.filter.Or;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Field;

public class SampleFilterGenerator extends Observable implements FilterGenerator {

  @Override
  public Filter generateFilter(Object propertyId, Object value) {
      if ("id".equals(propertyId)) {
          /* Create an 'equals' filter for the ID field */
          if (value != null && value instanceof String) {
              try {
                  return new Compare.Equal(propertyId,
                          Integer.parseInt((String) value));
              } catch (NumberFormatException ignored) {
                  // If no integer was entered, just generate default filter
              }
          }
      } else if ("checked".equals(propertyId)) {
          if (value != null && value instanceof Boolean) {
              if (Boolean.TRUE.equals(value)) {
                  return new Compare.Equal(propertyId, value);
              } else {
                  return new Or(new Compare.Equal(propertyId, true),
                          new Compare.Equal(propertyId, false));
              }
          }
      }
      // For other properties, use the default filter
      return null;
  }

  @Override
  public Filter generateFilter(Object propertyId, Field<?> originatingField) {
      // Use the default filter
      return null;
  }

  @Override
  public AbstractField<?> getCustomFilterComponent(Object propertyId) {
      // removed custom filter component for id
      if ("checked".equals(propertyId)) {
          CheckBox box = new CheckBox();
          return box;
      }
      return null;
  }

  @Override
  public void filterRemoved(Object propertyId) {
    setChanged();
    notifyObservers(); 
  }

  @Override
  public void filterAdded(Object propertyId,
          Class<? extends Filter> filterType, Object value) {
    setChanged();
    notifyObservers(); 
  }

  @Override
  public Filter filterGeneratorFailed(Exception reason, Object propertyId,
          Object value) {
      /* Return null -> Does not add any filter on failure */
      return null;
}

}
