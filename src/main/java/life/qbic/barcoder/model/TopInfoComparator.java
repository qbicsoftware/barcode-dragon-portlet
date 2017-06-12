package life.qbic.barcoder.model;


import java.util.Comparator;

/**
 * Compares IBarcodeBeans by the sample description
 * @author Andreas Friedrich
 *
 */
public class TopInfoComparator implements Comparator<IBarcodeBean> {

	private static final TopInfoComparator instance = 
			new TopInfoComparator();

	public static TopInfoComparator getInstance() {
		return instance;
	}

	private TopInfoComparator() {
	}

	@Override
	public int compare(IBarcodeBean o1, IBarcodeBean o2) {
		return o1.firstInfo().compareTo(o2.firstInfo());
	}

}
