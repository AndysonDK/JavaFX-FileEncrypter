package com.atmos.model;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;

import static com.atmos.model.Encryption.decryptFile;
import static com.atmos.model.Encryption.encryptFile;

public final class Storage {
    private static final String generalKey = "lKJ~w+7')>_]Fg/2p-CW=";

    private static int remainingTries;
    private static int failedTimes;
    private static String PASSWORD;

    private static File file;
    private static File encFile;

    Storage() throws IOException, URISyntaxException, CryptoException {
        remainingTries = 6;
        failedTimes = 1;
        file = new File(loadParentDirectory() + "/data");
        encFile = new File(loadParentDirectory() + "/data.enc");
        loadData();
    }

    public String loadParentDirectory() throws URISyntaxException {
        File jarFile = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        return jarFile.getParent();
    }

    boolean hasPassword() {
        return PASSWORD != null;
    }

    public String getPASSWORD() {
        return PASSWORD;
    }

    public void setPASSWORD(String PASSWORD) {
        Storage.PASSWORD = PASSWORD;
    }

    public int getFailedTimes() {
        return failedTimes;
    }

    public void setFailedTimes(int failedTimes) {
        Storage.failedTimes = failedTimes;
    }

    public int getRemainingTries() {
        return remainingTries;
    }

    public void setRemainingTries(int remainingTries) {
        Storage.remainingTries = remainingTries;
    }

    void saveData() throws CryptoException, IOException {
        FileOutputStream out = new FileOutputStream(file);
        DataOutputStream dataOut = new DataOutputStream(out);
        dataOut.writeInt(remainingTries);
        dataOut.writeInt(failedTimes);
        if (hasPassword()) dataOut.writeUTF(PASSWORD);
        out.close();
        dataOut.close();
        encryptFile(generalKey, file, encFile);
        if (!file.delete()) throw new IOException("Couldn't delete a specific file with data in it");
    }

    private void loadData() throws CryptoException, IOException {
        if (encFile.exists()) {
            if (encFile.length() > 0) {
                try {
                    decryptFile(generalKey, encFile, file);
                } catch (CryptoException exc) {
                    if (exc.getCause().getClass().equals(IllegalBlockSizeException.class)
                        || exc.getCause().getClass().equals(BadPaddingException.class)) {
                        if (!file.delete()) throw new IOException("Couldn't delete a specific file");
                        throw new CryptoException("The original content in the file: \"data.enc\" has been modified/changed", exc);
                    }
                    if (!file.delete()) throw new IOException("Couldn't delete a specific file");
                    throw exc;
                }
                FileInputStream in = new FileInputStream(file);
                DataInputStream dataIn = new DataInputStream(in);
                if (dataIn.available() > 0) remainingTries = dataIn.readInt();
                if (dataIn.available() > 0) failedTimes = dataIn.readInt();
                if (dataIn.available() > 0) PASSWORD = dataIn.readUTF();
                in.close();
                dataIn.close();
            }
            if (!encFile.delete()) throw new IOException("Couldn't delete the specific files");
            if (!file.delete()) throw new IOException("Couldn't delete the specific files");
        }
    }
}