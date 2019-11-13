package net.xmeter.samplers.mqtt.fuse;

import net.xmeter.samplers.mqtt.MQTTQoS;
import org.fusesource.mqtt.client.QoS;

class FuseUtil {
    static QoS map(MQTTQoS qos) {
        switch (qos) {
            case AT_MOST_ONCE: return QoS.AT_MOST_ONCE;
            case AT_LEAST_ONCE: return QoS.AT_LEAST_ONCE;
            case EXACTLY_ONCE: return QoS.EXACTLY_ONCE;
            default: throw new IllegalArgumentException("Unknown QoS: " + qos);
        }
    }
}
