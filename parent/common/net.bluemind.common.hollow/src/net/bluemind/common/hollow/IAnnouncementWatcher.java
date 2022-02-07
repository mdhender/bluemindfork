package net.bluemind.common.hollow;

import com.netflix.hollow.api.consumer.HollowConsumer.AnnouncementWatcher;

public interface IAnnouncementWatcher extends AnnouncementWatcher {

	boolean isListening();

}
