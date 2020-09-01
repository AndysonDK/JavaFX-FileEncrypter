package com.atmos;

import com.atmos.model.MainApp;
import it.sauronsoftware.junique.AlreadyLockedException;
import it.sauronsoftware.junique.JUnique;

public class Main {
    public static void main(String[] args) {
        String appId = "FileEncrypter:com.atmos.Main";
        try {
            JUnique.acquireLock(appId);
            System.out.printf("Start of program%n%n");
            MainApp.main(args);
            System.out.printf("%nEnd of program");
        } catch (AlreadyLockedException e) {
            System.out.println("Already one instance of this application is running!");
        }
    }
}
