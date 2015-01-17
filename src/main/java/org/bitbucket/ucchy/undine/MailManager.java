/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bitbucket.ucchy.undine.tellraw.ClickEventType;
import org.bitbucket.ucchy.undine.tellraw.MessageComponent;
import org.bitbucket.ucchy.undine.tellraw.MessageParts;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * メールデータマネージャ
 * @author ucchy
 */
public class MailManager {

    private static final String COMMAND = "/undine";
    private static final int PAGE_SIZE = 10;

    private ArrayList<MailData> mails;
    private HashMap<MailSender, MailData> editmodeMails;
    private int nextIndex;

    private Undine parent;

    /**
     * コンストラクタ
     */
    public MailManager(Undine parent) {
        this.parent = parent;
        this.editmodeMails = new HashMap<MailSender, MailData>();
        reload();
    }

    /**
     * メールデータを再読込する
     */
    public void reload() {

        mails = new ArrayList<MailData>();
        nextIndex = 1;

        File folder = parent.getMailFolder();
        File[] files = folder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".yml");
            }
        });

        for ( File file : files ) {
            MailData data = MailData.load(file);
            mails.add(data);

            if ( nextIndex <= data.getIndex() ) {
                nextIndex = data.getIndex() + 1;
            }
        }
    }

    /**
     * 指定されたインデクスのメールを取得する
     * @param index インデクス
     * @return メールデータ
     */
    public MailData getMail(int index) {

        for ( MailData m : mails ) {
            if ( m.getIndex() == index ) {
                return m;
            }
        }
        return null;
    }

    /**
     * 新しいテキストメールを送信する
     * @param from 送り元
     * @param to 宛先
     * @param message メッセージ
     */
    public void sendNewMail(CommandSender from, MailSender to, String message) {

        ArrayList<MailSender> toList = new ArrayList<MailSender>();
        toList.add(to);
        ArrayList<String> messageList = new ArrayList<String>();
        messageList.add(message);
        MailData mail = new MailData(toList, MailSender.getMailSender(from), messageList);
        sendNewMail(mail);
    }

    /**
     * 新しいテキストメールを送信する
     * @param from 送り元
     * @param to 宛先
     * @param message メッセージ
     */
    public void sendNewMail(CommandSender from, List<MailSender> to, String message) {

        ArrayList<String> messageList = new ArrayList<String>();
        messageList.add(message);
        MailData mail = new MailData(to, MailSender.getMailSender(from), messageList);
        sendNewMail(mail);
    }

    /**
     * 新しいテキストメールを送信する
     * @param from 送り元
     * @param to 宛先
     * @param message メッセージ
     */
    public void sendNewMail(CommandSender from, List<MailSender> to, List<String> message) {

        MailData mail = new MailData(to, MailSender.getMailSender(from), message);
        sendNewMail(mail);
    }

    /**
     * 新しいメールを送信する
     * @param mail メール
     */
    public void sendNewMail(MailData mail) {

        // 宛先の重複を削除して再設定する
        ArrayList<MailSender> to_copy = new ArrayList<MailSender>();
        for ( MailSender t : mail.getTo() ) {
            if ( !to_copy.contains(t) ) {
                to_copy.add(t);
            }
        }
        mail.setTo(to_copy);

        // インデクスを設定する
        mail.setIndex(nextIndex);
        nextIndex++;

        // 送信時間を設定する
        mail.setDate(new Date());
        mail.makeAttachmentsOriginal();

        // 保存する
        mails.add(mail);
        saveMail(mail);

        // 宛先の人がログイン中なら知らせる
        String msg = Messages.get("InformationYouGotMail",
                "%from", mail.getFrom().getName());
        for ( MailSender to : mail.getTo() ) {
            if ( to.isOnline() ) {
                to.getPlayer().sendMessage(msg);
            }
        }

        // 送信したことを送信元に知らせる
        if ( mail.getFrom() != null && mail.getFrom().isOnline() ) {
            msg = Messages.get("InformationYouSentMail");
            mail.getFrom().getPlayer().sendMessage(msg);
        }
    }

    /**
     * 受信したメールのリストを取得する
     * @param sender 取得する対象
     * @return メールのリスト
     */
    public ArrayList<MailData> getInboxMails(CommandSender sender) {

        MailSender ms = MailSender.getMailSender(sender);
        ArrayList<MailData> box = new ArrayList<MailData>();
        for ( MailData mail : mails ) {
            if ( mail.getTo().contains(ms) ) {
                box.add(mail);
            }
        }
        sortNewer(box);
        return box;
    }

    /**
     * 受信したメールで未読のリストを取得する
     * @param sender 取得する対象
     * @return メールのリスト
     */
    public ArrayList<MailData> getUnreadMails(CommandSender sender) {

        MailSender ms = MailSender.getMailSender(sender);
        ArrayList<MailData> box = new ArrayList<MailData>();
        for ( MailData mail : mails ) {
            if ( mail.getTo().contains(ms) && !mail.isRead(ms) ) {
                box.add(mail);
            }
        }
        sortNewer(box);
        return box;
    }

    /**
     * 送信したメールのリストを取得する
     * @param sender 取得する対象
     * @return メールのリスト
     */
    public ArrayList<MailData> getOutboxMails(CommandSender sender) {

        MailSender ms = MailSender.getMailSender(sender);
        ArrayList<MailData> box = new ArrayList<MailData>();
        for ( MailData mail : mails ) {
            if ( mail.getFrom().equals(ms) ) {
                box.add(mail);
            }
        }
        sortNewer(box);
        return box;
    }

    /**
     * 指定されたメールを開いて確認する
     * @param sender 確認する対象
     * @param mail メール
     */
    public void displayMail(CommandSender sender, MailData mail) {

        // 指定されたsenderの画面にメールを表示する
        for ( String line : mail.getDescription() ) {
            sender.sendMessage(line);
        }

        // 既読を付ける
        mail.setReadFlag(MailSender.getMailSender(sender));
        saveMail(mail);
    }

    /**
     * 指定されたメールの添付ボックスを開いて確認する
     * @param player 確認する人
     * @param mail メール
     */
    public void displayAttachBox(Player player, MailData mail) {
        parent.getBoxManager().displayAttachmentBox(player, mail);
    }

    /**
     * 指定されたメールデータをUndineに保存する
     * @param mail メールデータ
     */
    public void saveMail(MailData mail) {

        // 編集中で未送信のメールは保存できません。
        if ( mail.getIndex() == 0 ) {
            return;
        }

        String filename = String.format("%1$08d.yml", mail.getIndex());
        File folder = parent.getMailFolder();
        File file = new File(folder, filename);
        mail.save(file);
    }

    /**
     * 編集中メールを作成して返す
     * @param sender 取得対象のsender
     * @return 編集中メール（編集中でないならnull）
     */
    public MailData makeEditmodeMail(CommandSender sender) {
        MailSender ms = MailSender.getMailSender(sender);
        if ( editmodeMails.containsKey(ms) ) {
            return editmodeMails.get(ms);
        }
        MailData mail = new MailData();
        mail.setFrom(ms);
        editmodeMails.put(ms, mail);
        return mail;
    }

    /**
     * 編集中メールを取得する
     * @param sender 取得対象のsender
     * @return 編集中メール（編集中でないならnull）
     */
    public MailData getEditmodeMail(CommandSender sender) {
        MailSender ms = MailSender.getMailSender(sender);
        if ( editmodeMails.containsKey(ms) ) {
            return editmodeMails.get(ms);
        }
        return null;
    }

    /**
     * 編集中メールを削除する
     * @param sender 削除対象のsender
     */
    public void clearEditmodeMail(CommandSender sender) {
        editmodeMails.remove(MailSender.getMailSender(sender));
    }

    /**
     * 指定されたsenderに、Inboxリストを表示する。
     * @param sender 表示対象のsender
     * @param page 表示するページ
     */
    protected void displayInboxList(CommandSender sender, int page) {

        MailSender ms = MailSender.getMailSender(sender);

        String pre = Messages.get("InboxLinePre");

        ArrayList<MailData> mails = getInboxMails(sender);
        int max = (int)((mails.size() - 1) / PAGE_SIZE) + 1;
        int unread = 0;
        for ( MailData m : mails ) {
            if ( !m.isRead(ms) ) {
                unread++;
            }
        }

        String fline = Messages.get("InboxFirstLine", "%unread", unread + "");
        ms.sendMessage(fline);

        for ( int i=0; i<10; i++ ) {

            int index = (page - 1) * 10 + i;
            if ( index < 0 || mails.size() <= index ) {
                continue;
            }

            MailData mail = mails.get(index);
            ChatColor color = mail.isRead(ms) ? ChatColor.GRAY : ChatColor.GOLD;

            sendMailLine(ms, pre, color + mail.getInboxSummary(), mail);
        }

        sendPager(ms, COMMAND + " inbox", page, max);
    }

    /**
     * 指定されたsenderに、Outboxリストを表示する。
     * @param sender 表示対象のsender
     * @param page 表示するページ
     */
    protected void displayOutboxList(CommandSender sender, int page) {

        MailSender ms = MailSender.getMailSender(sender);

        String pre = Messages.get("OutboxLinePre");

        ArrayList<MailData> mails = getOutboxMails(sender);
        int max = (int)((mails.size() - 1) / PAGE_SIZE) + 1;

        String fline = Messages.get("OutboxFirstLine",
                new String[]{"%page", "%max"},
                new String[]{page + "", max + ""});
        ms.sendMessage(fline);

        for ( int i=0; i<10; i++ ) {

            int index = (page - 1) * 10 + i;
            if ( index < 0 || mails.size() <= index ) {
                continue;
            }

            MailData mail = mails.get(index);
            ChatColor color = ChatColor.GRAY;

            sendMailLine(ms, pre, color + mail.getOutboxSummary(), mail);
        }

        sendPager(ms, COMMAND + " outbox", page, max);
    }

    /**
     * メールデータのリストを、新しいメール順に並び替えする
     * @param list リスト
     */
    private static void sortNewer(List<MailData> list) {
        Collections.sort(list, new Comparator<MailData>() {
            public int compare(MailData o1, MailData o2) {
                return o2.getDate().compareTo(o1.getDate());
            }
        });
    }

    /**
     * メールサマリー表示を対象プレイヤーに表示する
     * @param sender 表示対象
     * @param pre プレフィックス
     * @param summary サマリーの文字列
     * @param mail メールデータ
     */
    private void sendMailLine(
            MailSender sender, String pre, String summary, MailData mail) {

        MessageComponent msg = new MessageComponent();

        msg.addText(pre);

        MessageParts button = new MessageParts(
                "[" + mail.getIndex() + "]", ChatColor.BLUE, ChatColor.UNDERLINE);
        button.setClickEvent(
                ClickEventType.RUN_COMMAND, COMMAND + " read " + mail.getIndex());
        msg.addParts(button);

        msg.addText(summary);

        msg.send(sender);
    }

    /**
     * ページャーを対象プレイヤーに表示する
     * @param sender 表示対象
     * @param commandPre コマンドのプレフィックス
     * @param page 現在のページ
     * @param max 最終ページ
     */
    private void sendPager(MailSender sender, String commandPre, int page, int max) {

        String firstLabel = Messages.get("FirstPage");
        String prevLabel = Messages.get("PrevPage");
        String nextLabel = Messages.get("NextPage");
        String lastLabel = Messages.get("LastPage");
        String firstToolTip = Messages.get("FirstPageToolTip");
        String prevToolTip = Messages.get("PrevPageToolTip");
        String nextToolTip = Messages.get("NextPageToolTip");
        String lastToolTip = Messages.get("LastPageToolTip");
        String parts = Messages.get("PagerParts");

        MessageComponent msg = new MessageComponent();

        msg.addText(parts + " ");

        if ( page > 1 ) {
            MessageParts firstButton = new MessageParts(
                    firstLabel, ChatColor.BLUE, ChatColor.UNDERLINE);
            firstButton.setClickEvent(ClickEventType.RUN_COMMAND, commandPre + " 1");
            firstButton.setHoverText(firstToolTip);
            msg.addParts(firstButton);

            msg.addText(" ");

            MessageParts prevButton = new MessageParts(
                    prevLabel, ChatColor.BLUE, ChatColor.UNDERLINE);
            prevButton.setClickEvent(ClickEventType.RUN_COMMAND, commandPre + " " + (page - 1));
            prevButton.setHoverText(prevToolTip);
            msg.addParts(prevButton);

        } else {
            msg.addText(firstLabel + " " + prevLabel, ChatColor.WHITE);

        }

        msg.addText(" (" + page + "/" + max + ") ");

        if ( page < max ) {
            MessageParts nextButton = new MessageParts(
                    nextLabel, ChatColor.BLUE, ChatColor.UNDERLINE);
            nextButton.setClickEvent(ClickEventType.RUN_COMMAND, commandPre + " " + (page + 1));
            nextButton.setHoverText(nextToolTip);
            msg.addParts(nextButton);

            msg.addText(" ");

            MessageParts lastButton = new MessageParts(
                    lastLabel, ChatColor.BLUE, ChatColor.UNDERLINE);
            lastButton.setClickEvent(ClickEventType.RUN_COMMAND, commandPre + " " + max);
            lastButton.setHoverText(lastToolTip);
            msg.addParts(lastButton);

        } else {
            msg.addText(nextLabel + " " + lastLabel, ChatColor.WHITE);
        }

        msg.addText(" " + parts);

        msg.send(sender);
    }
}