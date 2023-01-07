package com.binance.connector.client;

import com.binance.connector.client.impl.futures.Account;
import com.binance.connector.client.impl.futures.UserData;
import com.binance.connector.client.impl.futures.Market;
import com.binance.connector.client.impl.futures.PortfolioMargin;

public interface FuturesClient {
    Market market();
    Account account();
    UserData userData();
    PortfolioMargin portfolioMargin();
}
