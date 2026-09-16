package com.liujun.trade_ff.core.modle;

import com.binance.connector.client.utils.WebSocketConnection;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 记录某个平台的某种数据流的状态。例如depth
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WebSocketState {
    public enum StreamType {
        depth
    }

    /**
     * websocket返回的connectionId
     */
    private Integer connectionId = 0;

    /**
     * 最后一次收到数据时的时间（毫秒）
     */
    private Long lastUpdateTime = 0L;

    /**
     * 平台传递数据时，附带过来的id
     */
    private Long lastUpdateId = 0L;
    private WebSocketConnection webSocketConnection;
}
