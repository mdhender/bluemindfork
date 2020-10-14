package net.bluemind.authentication.service.tokens;

import com.netflix.hollow.api.objects.delegate.HollowObjectDelegate;


@SuppressWarnings("all")
public interface TokenDelegate extends HollowObjectDelegate {

    public String getKey(int ordinal);

    public boolean isKeyEqual(int ordinal, String testValue);

    public int getSubjectUidOrdinal(int ordinal);

    public int getSubjectDomainOrdinal(int ordinal);

    public String getOrigin(int ordinal);

    public boolean isOriginEqual(int ordinal, String testValue);

    public long getExpiresTimestamp(int ordinal);

    public Long getExpiresTimestampBoxed(int ordinal);

    public TokenTypeAPI getTypeAPI();

}