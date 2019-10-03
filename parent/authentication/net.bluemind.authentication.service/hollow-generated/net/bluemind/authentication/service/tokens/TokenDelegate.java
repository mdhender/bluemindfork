package net.bluemind.authentication.service.tokens;

import com.netflix.hollow.api.objects.delegate.HollowObjectDelegate;


@SuppressWarnings("all")
public interface TokenDelegate extends HollowObjectDelegate {

    public int getKeyOrdinal(int ordinal);

    public int getSubjectUidOrdinal(int ordinal);

    public int getSubjectDomainOrdinal(int ordinal);

    public long getExpiresTimestamp(int ordinal);

    public Long getExpiresTimestampBoxed(int ordinal);

    public TokenTypeAPI getTypeAPI();

}