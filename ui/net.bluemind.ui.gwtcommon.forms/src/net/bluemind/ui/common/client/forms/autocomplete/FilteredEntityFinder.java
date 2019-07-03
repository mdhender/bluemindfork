package net.bluemind.ui.common.client.forms.autocomplete;

import java.util.Collection;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.ListResult;

public abstract class FilteredEntityFinder<T, TQ> implements IEntityFinder<T, TQ> {

	private IEntityFinder<T, TQ> filteredFinder;

	public String getType(T result) {
		return filteredFinder.getType(result);
	}

	public String toString(T result) {
		return filteredFinder.toString(result);
	}

	public void find(TQ tQuery, final AsyncHandler<ListResult<T>> cb) {
		filteredFinder.find(tQuery, new AsyncHandler<ListResult<T>>() {

			@Override
			public void success(ListResult<T> result) {
				cb.success(filterResult(result));
			}

			@Override
			public void failure(Throwable caught) {
				cb.failure(caught);
			}

		});
	}

	protected abstract ListResult<T> filterResult(ListResult<T> result);

	public TQ queryFromString(String queryString) {
		return filteredFinder.queryFromString(queryString);
	}

	public void reload(Collection<T> ids,
			net.bluemind.ui.common.client.forms.autocomplete.IEntityFinder.ReloadCb<T> cb) {
		filteredFinder.reload(ids, cb);
	}

	public FilteredEntityFinder(IEntityFinder<T, TQ> filteredFinder) {
		this.filteredFinder = filteredFinder;
	}
}