package PNCUtilityUI;

import java.io.Serializable;

public class RecorderSettings implements Serializable {
    public int maxDrawingRecords;
    public String DBAliasName;
    public String DBFilePath;
    public String DBNameOrIP;
    public String DBPort;
    public boolean useAlias;
    public boolean allowHeaderReposition;
    public int maxInjectionWithoutConf;

    public String regExpTruncate;
    public String prodMgrAliasName;
    public String prodMgrLogin;
    public String prodMgrPass;
    public String prodMgrNameOrIP;
    public String prodMgrPort;


    public RecorderSettings(int maxDrawingRecords, String DBAliasName, String DBFilePath, String DBNameOrIP,
                            String DBPort, boolean useAlias, boolean ahr, int miwc) {
        this.maxDrawingRecords = maxDrawingRecords;
        this.DBAliasName = DBAliasName;
        this.DBFilePath = DBFilePath;
        this.useAlias = useAlias;
        this.allowHeaderReposition = ahr;
        this.maxInjectionWithoutConf = miwc;
        this.DBNameOrIP = DBNameOrIP;
        this.DBPort = DBPort;
    }
}
