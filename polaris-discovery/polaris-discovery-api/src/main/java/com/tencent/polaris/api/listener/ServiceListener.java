package com.tencent.polaris.api.listener;

import com.tencent.polaris.api.pojo.ServiceChangeEvent;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public interface ServiceListener {

    /**
     *
     * @param event {@link ServiceChangeEvent}
     */
    void onEvent(ServiceChangeEvent event);

}
