/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine;

import java.io.File;
import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Undineコンフィグ管理クラス
 * @author ucchy
 */
public class UndineConfig {

    /** メッセージ言語 */
    private String lang;

    /** メールにアイテムの添付を可能にするかどうか */
    private boolean enableAttachment;

    /** 送信に料金を必要とするか */
    private boolean enableSendFee;

    /** 送信に必要な料金、送信先が複数ある場合は、送信先ごとに課金される */
    private int sendFee;

    /** 添付アイテムを1件付けるのに必要な料金 */
    private int attachFee;

    /** 自分自身に送信を可能とするか */
    private boolean enableSendSelf;

    /** 添付ボックス操作不可とするワールド */
    private List<String> disableWorldsToOpenAttachBox;

    /** 添付ボックスのサイズ */
    private int attachBoxSize;

    private Undine parent;

    /**
     * コンストラクタ
     * @param parent プラグイン
     */
    public UndineConfig(Undine parent) {
        this.parent = parent;
        reloadConfig();
    }

    /**
     * コンフィグを読み込む
     */
    protected void reloadConfig() {

        if ( !parent.getDataFolder().exists() ) {
            parent.getDataFolder().mkdirs();
        }

        File file = new File(parent.getDataFolder(), "config.yml");
        if ( !file.exists() ) {
            if ( parent.getReleaseLang().equals("ja") ) {
                Utility.copyFileFromJar(
                        parent.getJarFile(), file, "config_ja.yml", false);
            } else {
                Utility.copyFileFromJar(
                        parent.getJarFile(), file, "config.yml", false);
            }
        }

        parent.reloadConfig();
        FileConfiguration conf = parent.getConfig();

        // 読み込み
        lang = conf.getString("lang", "ja");
        enableAttachment = conf.getBoolean("enableAttachment", true);
        enableSendFee = conf.getBoolean("enableSendFee", false);
        sendFee = conf.getInt("sendFee", 10);
        attachFee = conf.getInt("attachFee", 10);
        enableSendSelf = conf.getBoolean("enableSendSelf", false);
        disableWorldsToOpenAttachBox =
                conf.getStringList("disableWorldsToOpenAttachBox");
        attachBoxSize = conf.getInt("attachBoxSize", 1);

        // sendFeeは、マイナスが指定されていたら0に変更する
        if ( sendFee < 0 ) {
            sendFee = 0;
        }

        // 添付ボックスは、1から6までの数値に制限する
        if ( attachBoxSize < 1 ) {
            attachBoxSize = 1;
        } else if ( attachBoxSize > 6 ) {
            attachBoxSize = 6;
        }
    }

    /**
     * @return lang
     */
    public String getLang() {
        return lang;
    }

    /**
     * @return enableAttachment
     */
    public boolean isEnableAttachment() {
        return enableAttachment;
    }

    /**
     * @return enableSendFee
     */
    public boolean isEnableSendFee() {
        return enableSendFee;
    }

    /**
     * @return sendFee
     */
    public int getSendFee() {
        return sendFee;
    }

    /**
     * @return attachFee
     */
    public int getAttachFee() {
        return attachFee;
    }

    /**
     * @return enableSendSelf
     */
    public boolean isEnableSendSelf() {
        return enableSendSelf;
    }

    /**
     * @return disableWorldsToOpenAttachment
     */
    public List<String> getDisableWorldsToOpenAttachBox() {
        return disableWorldsToOpenAttachBox;
    }

    /**
     * @return attachBoxSize
     */
    public int getAttachBoxSize() {
        return attachBoxSize;
    }
}
