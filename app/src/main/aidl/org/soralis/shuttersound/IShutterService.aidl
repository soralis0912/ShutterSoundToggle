package org.soralis.shuttersound;

interface IShutterService {
    // Shizuku calls this transaction to stop the user service.
    void destroy() = 16777114;

    int getForSystem() = 1;
    void setForSystem(int config) = 2;
}
