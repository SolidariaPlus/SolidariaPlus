package com.solidariaplus.identity.identityaccess.application.port.output;

public interface RequestRateCounter {
    /** Counts one request from the address in the given minute and returns the requests counted so far. */
    long count(String address, long epochMinute);
}
