package net.bluemind.startup.services;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public interface SortableService {

	int rank();

	@SuppressWarnings("unchecked")
	default <T extends SortableService> void insertSorted(List<T> sortedServices) {
		int index = Collections.binarySearch(sortedServices, (T) this, Comparator.comparing(SortableService::rank));
		if (index < 0) {
			index = -index - 1;
		}
		sortedServices.add(index, (T) this);
	}
}
