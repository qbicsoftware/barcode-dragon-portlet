package life.qbic.portal.portlet.model;


import java.util.Comparator;

/**
 * Compares IBarcodeBeans by sample ID
 * @author Andreas Friedrich
 *
 */
public class ImportantStringComparator implements Comparator<IBarcodeBean> {

	private static final ImportantStringComparator instance = 
			new ImportantStringComparator();

	public static ImportantStringComparator getInstance() {
		return instance;
	}

	private ImportantStringComparator() {
	}

	@Override
	public int compare(IBarcodeBean o1, IBarcodeBean o2) {
		return o1.getCodedString().compareTo(o2.getCodedString());
	}

}
