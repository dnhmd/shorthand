package com.shorthand.consumer.infrastructure.adapter.outbound.geoip;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CountryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;

public class GeoIpAdapter {

    private static final Logger log = LoggerFactory.getLogger(GeoIpAdapter.class);

    private final DatabaseReader databaseReader;

    public GeoIpAdapter(DatabaseReader databaseReader) {
        this.databaseReader = databaseReader;
    }

    public String resolveCountry(String ipAddress) {
        try {
            InetAddress inetAddress = InetAddress.getByName(ipAddress);
            CountryResponse countryResponse = databaseReader.country(inetAddress);
            return countryResponse.getCountry().getName();
        } catch (IOException | GeoIp2Exception e) {
            log.warn("GeoIP | IP: {} | Country Resolution Failed | Error: {}", ipAddress, e.getMessage());
            return "Unknown";
        }
    }
}
