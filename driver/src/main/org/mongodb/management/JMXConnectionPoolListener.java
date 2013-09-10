/*
 * Copyright (c) 2008 - 2013 10gen, Inc. <http://10gen.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mongodb.management;

import org.mongodb.connection.ServerAddress;
import org.mongodb.event.ConnectionEvent;
import org.mongodb.event.ConnectionPoolEvent;
import org.mongodb.event.ConnectionPoolListener;
import org.mongodb.event.ConnectionPoolOpenedEvent;
import org.mongodb.event.ConnectionPoolWaitQueueEvent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.String.format;

/**
 * A connection pool listener that manages a set of JMX MBeans, one for each connection pool.
 *
 * @since 3.0
 */
public class JMXConnectionPoolListener implements ConnectionPoolListener {
    private final ConcurrentMap<ClusterIdServerAddressPair, ConnectionPoolStatistics> map =
            new ConcurrentHashMap<ClusterIdServerAddressPair, ConnectionPoolStatistics>();

    public String getMBeanObjectName(final String clusterId, final ServerAddress serverAddress) {
        return format("org.mongodb.driver:type=ConnectionPool,clusterId=%s,host=%s,port=%s", clusterId, serverAddress.getHost(),
                serverAddress.getPort());
    }

    public ConnectionPoolStatisticsMBean getMBean(final String clusterId, final ServerAddress serverAddress) {
        return getStatistics(clusterId, serverAddress);
    }

    @Override
    public void connectionPoolOpened(final ConnectionPoolOpenedEvent event) {
        ConnectionPoolStatistics statistics = new ConnectionPoolStatistics(event);
        map.put(new ClusterIdServerAddressPair(event.getClusterId(), event.getServerAddress()), statistics);
        MBeanServerFactory.getMBeanServer().registerMBean(statistics, getMBeanObjectName(event.getClusterId(), event.getServerAddress()));
    }

    @Override
    public void connectionPoolClosed(final ConnectionPoolEvent event) {
        map.remove(new ClusterIdServerAddressPair(event.getClusterId(), event.getServerAddress()));
        MBeanServerFactory.getMBeanServer().unregisterMBean(getMBeanObjectName(event.getClusterId(), event.getServerAddress()));
    }

    @Override
    public void connectionCheckedOut(final ConnectionEvent event) {
        getStatistics(event).connectionCheckedOut(event);
    }

    @Override
    public void connectionCheckedIn(final ConnectionEvent event) {
        getStatistics(event).connectionCheckedIn(event);
    }

    @Override
    public void waitQueueEntered(final ConnectionPoolWaitQueueEvent event) {
        getStatistics(event).waitQueueEntered(event);
    }

    @Override
    public void waitQueueExited(final ConnectionPoolWaitQueueEvent event) {
        getStatistics(event).waitQueueExited(event);
    }

    @Override
    public void connectionAdded(final ConnectionEvent event) {
        getStatistics(event).connectionAdded(event);
    }

    @Override
    public void connectionRemoved(final ConnectionEvent event) {
        getStatistics(event).connectionRemoved(event);
    }

    private ConnectionPoolStatistics getStatistics(final ConnectionEvent event) {
        return getStatistics(event.getClusterId(), event.getServerAddress());
    }

    private ConnectionPoolListener getStatistics(final ConnectionPoolEvent event) {
        return getStatistics(event.getClusterId(), event.getServerAddress());
    }

    private ConnectionPoolStatistics getStatistics(final String clusterId, final ServerAddress serverAddress) {
        return map.get(new ClusterIdServerAddressPair(clusterId, serverAddress));
    }

    private static final class ClusterIdServerAddressPair {
        private final String clusterId;
        private final ServerAddress serverAddress;

        private ClusterIdServerAddressPair(final String clusterId, final ServerAddress serverAddress) {
            this.clusterId = clusterId;
            this.serverAddress = serverAddress;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final ClusterIdServerAddressPair that = (ClusterIdServerAddressPair) o;

            if (!clusterId.equals(that.clusterId)) {
                return false;
            }

            if (!serverAddress.equals(that.serverAddress)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = clusterId.hashCode();
            result = 31 * result + serverAddress.hashCode();
            return result;
        }
    }
}
