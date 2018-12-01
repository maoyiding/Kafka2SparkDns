package com.dns.utils;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.net.InetAddress;
import java.net.UnknownHostException;


public class ESClient {

    public static TransportClient esClient() throws UnknownHostException {

        Settings setting = Settings.builder()
                .put("cluster.name", "test")
                .put("client.transport.sniff", true)
                .build();

        InetSocketTransportAddress master = new InetSocketTransportAddress(
                InetAddress.getByName("localhost"),9300);

        TransportClient client = new PreBuiltTransportClient(setting).addTransportAddress(master);

        return client;

    }

}
